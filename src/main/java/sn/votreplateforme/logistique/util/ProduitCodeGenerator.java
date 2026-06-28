package sn.votreplateforme.logistique.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sn.votreplateforme.logistique.repository.ProduitRepository;

/**
 * Générateur de codes produit uniques.
 * Format: DKS-XXXXX (ex: DKS-00001)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProduitCodeGenerator {

    private static final String PREFIX = "DKS-";

    private final ProduitRepository produitRepository;

    public String generate() {
        long count = produitRepository.countByCodeStartingWith(PREFIX);
        long next = count + 1;
        String code = PREFIX + String.format("%05d", next);
        // Sécurité : éviter une collision improbable
        while (produitRepository.existsByCode(code)) {
            next++;
            code = PREFIX + String.format("%05d", next);
        }
        return code;
    }
}
