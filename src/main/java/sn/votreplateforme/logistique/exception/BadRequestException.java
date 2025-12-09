package sn.votreplateforme.logistique.exception;

/**
 * Exception levée lors d'une requête invalide (400 Bad Request)
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
