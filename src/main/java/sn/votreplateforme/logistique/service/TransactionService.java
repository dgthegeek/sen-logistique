package sn.votreplateforme.logistique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.votreplateforme.logistique.dto.*;
import sn.votreplateforme.logistique.entity.Transaction;
import sn.votreplateforme.logistique.entity.Vendeur;
import sn.votreplateforme.logistique.exception.BusinessException;
import sn.votreplateforme.logistique.exception.ResourceNotFoundException;
import sn.votreplateforme.logistique.repository.TransactionRepository;
import sn.votreplateforme.logistique.repository.VendeurRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Transaction - Gestion des transactions financières
 * 
 * Responsabilités :
 * - Enregistrement des paiements vendeurs
 * - Génération des références de transaction
 * - Historique des transactions avec pagination
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final VendeurRepository vendeurRepository;
    private final FinanceCalculator financeCalculator;
    private final TelegramService telegramService;
    
    /**
     * Effectue un paiement à un vendeur
     * 
     * @param vendeurId ID du vendeur
     * @param request Données du paiement (montant, commentaire)
     * @return AdminFinancesPayerVendeurVendeurIdPost200Response avec confirmation
     */
    @Transactional
    public AdminFinancesPayerVendeurVendeurIdPost200Response payerVendeur(
        Long vendeurId,
        AdminFinancesPayerVendeurVendeurIdPostRequest request
    ) {
        log.info("Paiement vendeur {}", vendeurId);

        // 1. Récupérer le vendeur
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Vendeur non trouvé: " + vendeurId
            ));

        // 2. Recalculer le solde disponible côté serveur (source unique, jamais le champ stocké
        //    ni le montant transmis par le client). L'admin solde TOUT le disponible d'un coup.
        BigDecimal soldeDisponible = financeCalculator.soldeDisponible(vendeur);
        if (soldeDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Aucun solde à payer pour ce vendeur");
        }

        // 3. Générer la référence de transaction
        String reference = genererReferenceTransaction();
        String commentaire = request != null && request.getCommentaire() != null
                ? request.getCommentaire()
                : "Paiement du solde disponible";

        // 4. Solder le vendeur : réutiliser la demande EN_ATTENTE si elle existe
        //    (pour ne pas créer un doublon), sinon créer une nouvelle transaction.
        //    Un seul PAIEMENT_VENDEUR EFFECTUE est ajouté, pour le montant exact du solde.
        Transaction paiement = transactionRepository
            .findFirstByVendeurAndTypeAndStatutOrderByDateTransactionAsc(
                vendeur,
                Transaction.TypeTransaction.PAIEMENT_VENDEUR,
                Transaction.StatutPaiement.EN_ATTENTE
            )
            .orElseGet(Transaction::new);

        paiement.setVendeur(vendeur);
        paiement.setType(Transaction.TypeTransaction.PAIEMENT_VENDEUR);
        paiement.setMontant(soldeDisponible);
        paiement.setReference(reference);
        paiement.setStatut(Transaction.StatutPaiement.EFFECTUE);
        paiement.setCommentaire(commentaire);
        paiement.setDateTransaction(LocalDateTime.now());

        transactionRepository.save(paiement);

        // 5. Synchroniser le champ legacy (le solde est désormais calculé dynamiquement,
        //    mais on garde le champ cohérent à 0 après solde complet).
        vendeur.setSoldeEnAttente(BigDecimal.ZERO);
        vendeurRepository.save(vendeur);

        log.info("✅ Paiement effectué - Ref: {} - Montant soldé: {} FCFA - Solde remis à 0",
            reference, soldeDisponible);

        telegramService.notifyVendeur(vendeur, String.format(
                "💰 <b>Paiement approuvé</b>%nMontant versé : %s FCFA%nRéférence : %s",
                soldeDisponible.toBigInteger(), reference));

        // 6. Construire la réponse
        AdminFinancesPayerVendeurVendeurIdPost200Response response =
            new AdminFinancesPayerVendeurVendeurIdPost200Response();
        response.setMessage(
            String.format("Paiement de %,d FCFA effectué", soldeDisponible.longValue())
        );
        response.setReference(reference);

        return response;
    }
    
    /**
     * Récupère l'historique des transactions avec pagination et filtres
     * 
     * @param vendeurId Filtre par vendeur (optionnel)
     * @param dateDebut Filtre date début (optionnel)
     * @param dateFin Filtre date fin (optionnel)
     * @param page Numéro de page
     * @param size Taille de page
     * @return PageTransaction avec historique
     */
    @Transactional(readOnly = true)
    public PageTransaction getHistoriqueTransactions(
        Long vendeurId,
        LocalDate dateDebut,
        LocalDate dateFin,
        Integer page,
        Integer size
    ) {
        log.info("Récupération historique transactions - VendeurId: {}, Page: {}", 
            vendeurId, page);
        
        // 1. Créer le Pageable
        Pageable pageable = PageRequest.of(
            page != null ? page : 0, 
            size != null ? size : 50,
            Sort.by(Sort.Direction.DESC, "dateTransaction")
        );
        
        // 2. Récupérer les transactions selon les filtres
        Page<Transaction> transactionsPage;
        
        if (vendeurId != null) {
            Vendeur vendeur = vendeurRepository.findById(vendeurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Vendeur non trouvé: " + vendeurId
                ));
            
            if (dateDebut != null && dateFin != null) {
                transactionsPage = transactionRepository.findByVendeurAndDateTransactionBetween(
                    vendeur,
                    dateDebut.atStartOfDay(),
                    dateFin.atTime(23, 59, 59),
                    pageable
                );
            } else {
                transactionsPage = transactionRepository.findByVendeur(vendeur, pageable);
            }
        } else if (dateDebut != null && dateFin != null) {
            transactionsPage = transactionRepository.findByDateTransactionBetween(
                dateDebut.atStartOfDay(),
                dateFin.atTime(23, 59, 59),
                pageable
            );
        } else {
            transactionsPage = transactionRepository.findAll(pageable);
        }
        
        // 3. Mapper vers les DTOs
        List<PageTransactionContentInner> content = transactionsPage.getContent().stream()
            .map(this::mapTransactionToDto)
            .collect(Collectors.toList());
        
        // 4. Construire la réponse PageTransaction
        PageTransaction response = new PageTransaction();
        response.setContent(content);
        response.setPage(transactionsPage.getNumber());
        response.setSize(transactionsPage.getSize());
        response.setTotalElements((int) transactionsPage.getTotalElements());
        response.setTotalPages(transactionsPage.getTotalPages());
        
        log.info("Historique récupéré - {} transactions", content.size());
        
        return response;
    }
    
    /**
     * Mappe une Transaction vers PageTransactionContentInner DTO
     * 
     * @param transaction Transaction entity
     * @return PageTransactionContentInner DTO
     */
    private PageTransactionContentInner mapTransactionToDto(Transaction transaction) {
        PageTransactionContentInner dto = new PageTransactionContentInner();
        dto.setId(transaction.getId());
        
        // Nom complet du vendeur
        String nomVendeur = transaction.getVendeur().getPrenom() + " " + 
                           transaction.getVendeur().getNom();
        dto.setVendeur(nomVendeur);
        
        dto.setMontant(transaction.getMontant());
        dto.setType(
            PageTransactionContentInner.TypeEnum.valueOf(transaction.getType().name())
        );
        dto.setReference(transaction.getReference());
        dto.setStatut(
            PageTransactionContentInner.StatutEnum.valueOf(transaction.getStatut().name())
        );
        
        // Convertir LocalDateTime en OffsetDateTime
        dto.setDate(
            transaction.getDateTransaction()
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
        );
        
        return dto;
    }
    
    /**
     * Génère une référence unique de transaction
     * Format: PAY-YYYYMMDD-XXX
     * 
     * @return Référence unique
     */
    private String genererReferenceTransaction() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Compter les transactions du jour
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour = LocalDate.now().atTime(23, 59, 59);
        
        long nombreTransactionsJour = transactionRepository
            .countByDateTransactionBetween(debutJour, finJour);
        
        // Incrémenter et formater sur 3 chiffres
        String numero = String.format("%03d", nombreTransactionsJour + 1);
        
        return "PAY-" + dateStr + "-" + numero;
    }
}
