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
        log.info("Paiement vendeur {} - Montant: {} FCFA", vendeurId, request.getMontant());
        
        // 1. Récupérer le vendeur
        Vendeur vendeur = vendeurRepository.findById(vendeurId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Vendeur non trouvé: " + vendeurId
            ));
        
        // 2. Vérifier que le montant ne dépasse pas le solde en attente
        if (request.getMontant().compareTo(vendeur.getSoldeEnAttente()) > 0) {
            throw new BusinessException(
                String.format(
                    "Le montant à payer (%s FCFA) dépasse le solde en attente (%s FCFA)",
                    request.getMontant(),
                    vendeur.getSoldeEnAttente()
                )
            );
        }
        
        // 3. Générer la référence de transaction
        String reference = genererReferenceTransaction();
        
        // 4. Créer la transaction
        Transaction transaction = Transaction.builder()
            .vendeur(vendeur)
            .montant(request.getMontant())
            .type(Transaction.TypeTransaction.PAIEMENT_VENDEUR)
            .reference(reference)
            .statut(Transaction.StatutPaiement.EFFECTUE)
            .commentaire(request.getCommentaire())
            .dateTransaction(LocalDateTime.now())
            .build();
        
        transactionRepository.save(transaction);
        
        // 5. Mettre à jour le solde du vendeur
        BigDecimal nouveauSolde = vendeur.getSoldeEnAttente().subtract(request.getMontant());
        vendeur.setSoldeEnAttente(nouveauSolde);
        vendeurRepository.save(vendeur);
        
        log.info("✅ Paiement effectué - Ref: {} - Nouveau solde vendeur: {} FCFA", 
            reference, nouveauSolde);
        
        // 6. Construire la réponse
        AdminFinancesPayerVendeurVendeurIdPost200Response response = 
            new AdminFinancesPayerVendeurVendeurIdPost200Response();
        response.setMessage(
            String.format("Paiement de %,d FCFA effectué", request.getMontant().longValue())
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
