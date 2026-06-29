package sn.votreplateforme.logistique;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

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

    public static void main(String[] args) {
        SpringApplication.run(LogistiqueDakarApplication.class, args);
    }
}
