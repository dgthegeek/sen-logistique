package sn.votreplateforme.logistique;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * Plateforme Logistique Dakar - Application principale
 *
 *
 * @author Dame Gaye
 * @version 1.0.0-MVP
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class LogistiqueDakarApplication {

    /**
     * Force le fuseau horaire de l'application sur celui du Sénégal (Africa/Dakar = UTC+0),
     * quelle que soit la configuration du serveur. Ainsi LocalDateTime.now() et les
     * horodatages affichés correspondent à l'heure locale de Dakar.
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Dakar"));
    }

    public static void main(String[] args) {
        SpringApplication.run(LogistiqueDakarApplication.class, args);
    }
}
