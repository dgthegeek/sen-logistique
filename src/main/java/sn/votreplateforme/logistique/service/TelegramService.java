package sn.votreplateforme.logistique.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.entity.User;
import sn.votreplateforme.logistique.repository.CloseurRepository;
import sn.votreplateforme.logistique.repository.DispatcheurRepository;
import sn.votreplateforme.logistique.repository.UserRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Notifications Telegram (bot) pour <b>tous les rôles</b> de la ligne de livraison
 * (vendeur, closeur, dispatcheur, livreur, admin).
 *
 * <p>Fonctionne uniquement si {@code telegram.enabled=true} et qu'un token est fourni.
 * Envoi best-effort (hors transaction, jamais bloquant pour l'appelant).
 * La liaison du compte se fait par polling de getUpdates : l'utilisateur ouvre
 * {@code t.me/<bot>?start=<code>} et on associe le chat au bon compte.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramService {

    private final UserRepository userRepository;
    private final CloseurRepository closeurRepository;
    private final DispatcheurRepository dispatcheurRepository;
    private final ObjectMapper objectMapper;

    @Value("${telegram.enabled:false}")
    private boolean enabled;
    @Value("${telegram.bot-token:}")
    private String botToken;
    @Value("${telegram.bot-username:}")
    private String botUsername;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "telegram-sender");
        t.setDaemon(true);
        return t;
    });

    private long lastUpdateId = 0L;

    public boolean isConfigured() {
        return enabled && botToken != null && !botToken.isBlank();
    }

    public String getBotUsername() {
        return botUsername;
    }

    // ==================== STATUT / LIAISON ====================

    /** Statut de liaison de l'utilisateur ; génère un code + deep link si pas encore lié. */
    @Transactional
    public sn.votreplateforme.logistique.dto.TelegramStatut getStatut(User user) {
        sn.votreplateforme.logistique.dto.TelegramStatut dto =
                new sn.votreplateforme.logistique.dto.TelegramStatut();
        dto.setEnabled(isConfigured());
        dto.setBotUsername(botUsername);

        boolean lie = user.getTelegramChatId() != null;
        dto.setLie(lie);

        if (!lie && isConfigured()) {
            String code = user.getTelegramLinkCode();
            if (code == null || code.isBlank()) {
                code = "u" + user.getId() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
                user.setTelegramLinkCode(code);
                userRepository.save(user);
            }
            dto.setDeepLink("https://t.me/" + botUsername + "?start=" + code);
        }
        return dto;
    }

    @Transactional
    public sn.votreplateforme.logistique.dto.TelegramStatut delier(User user) {
        user.setTelegramChatId(null);
        user.setTelegramLinkCode(null);
        userRepository.save(user);
        return getStatut(user);
    }

    // ==================== ENVOI ====================

    /** Notifie un utilisateur (ne fait rien s'il n'a pas lié Telegram ou si le bot est off). */
    public void notifyUser(User user, String message) {
        if (user == null || user.getTelegramChatId() == null || !isConfigured()) {
            return;
        }
        final String chatId = user.getTelegramChatId();
        sender.submit(() -> sendMessage(chatId, message));
    }

    /** Alias historique : notifie un vendeur (délègue à notifyUser). */
    public void notifyVendeur(User vendeur, String message) {
        notifyUser(vendeur, message);
    }

    /** Notifie tous les closeurs actifs ayant lié Telegram (nouvelle commande à prendre en charge). */
    public void notifyCloseurs(String message) {
        if (!isConfigured()) {
            return;
        }
        closeurRepository.findByActifTrueAndTelegramChatIdIsNotNull()
                .forEach(c -> notifyUser(c, message));
    }

    /** Notifie tous les dispatcheurs actifs ayant lié Telegram (commande prête à livrer). */
    public void notifyDispatcheurs(String message) {
        if (!isConfigured()) {
            return;
        }
        dispatcheurRepository.findByActifTrueAndTelegramChatIdIsNotNull()
                .forEach(d -> notifyUser(d, message));
    }

    private void sendMessage(String chatId, String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "HTML",
                    "disable_web_page_preview", true
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Telegram sendMessage {} -> HTTP {} : {}", chatId, resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warn("Échec envoi Telegram à {} : {}", chatId, e.getMessage());
        }
    }

    // ==================== LIAISON (polling getUpdates) ====================

    @Scheduled(fixedDelayString = "${telegram.poll-interval-ms:15000}")
    @Transactional
    public void pollUpdates() {
        if (!isConfigured()) {
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + botToken
                    + "/getUpdates?timeout=0&allowed_updates=%5B%22message%22%5D&offset=" + (lastUpdateId + 1);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                return;
            }
            JsonNode root = objectMapper.readTree(resp.body());
            if (!root.path("ok").asBoolean(false)) {
                return;
            }
            for (JsonNode upd : root.path("result")) {
                long updateId = upd.path("update_id").asLong();
                if (updateId > lastUpdateId) {
                    lastUpdateId = updateId;
                }
                JsonNode message = upd.path("message");
                String text = message.path("text").asText("");
                String chatId = message.path("chat").path("id").asText(null);
                if (chatId != null && text.startsWith("/start")) {
                    String[] parts = text.trim().split("\\s+", 2);
                    if (parts.length == 2) {
                        lierParCode(parts[1].trim(), chatId);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Telegram getUpdates: {}", e.getMessage());
        }
    }

    private void lierParCode(String code, String chatId) {
        userRepository.findByTelegramLinkCode(code).ifPresent(user -> {
            user.setTelegramChatId(chatId);
            user.setTelegramLinkCode(null);
            userRepository.save(user);
            log.info("Utilisateur {} ({}) lié à Telegram (chat {})", user.getId(), user.getRole(), chatId);
            sender.submit(() -> sendMessage(chatId,
                    "✅ <b>Dioks</b> — votre compte est bien lié. Vous recevrez vos notifications ici."));
        });
    }
}
