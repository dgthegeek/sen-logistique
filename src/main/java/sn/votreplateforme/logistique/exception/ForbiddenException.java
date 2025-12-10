package sn.votreplateforme.logistique.exception;

/**
 * Exception levée quand un utilisateur tente d'accéder à une ressource
 * pour laquelle il n'a pas l'autorisation (403 Forbidden)
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}