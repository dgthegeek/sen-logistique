package sn.votreplateforme.logistique.exception;

/**
 * Exception levée quand une ressource demandée n'existe pas (404 Not Found)
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}