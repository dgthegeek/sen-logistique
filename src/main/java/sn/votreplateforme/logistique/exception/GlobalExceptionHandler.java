package sn.votreplateforme.logistique.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import sn.votreplateforme.logistique.dto.ErrorResponse;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions
 * Intercepte toutes les exceptions et retourne des réponses JSON formatées
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Gestion des erreurs de validation (@Valid)
     * Retourne 400 Bad Request avec les détails des champs invalides
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Erreur de validation: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Validation Failed");
        errorResponse.setMessage("Données invalides: " + errors.toString());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }
    
    /**
     * Gestion des erreurs d'authentification (mauvais mot de passe)
     * Retourne 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            WebRequest request) {
        
        log.warn("Tentative de connexion avec des identifiants invalides");
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setError("Unauthorized");
        errorResponse.setMessage("Téléphone ou mot de passe incorrect");
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(errorResponse);
    }
    
    /**
     * Gestion utilisateur non trouvé
     * Retourne 401 Unauthorized (pour ne pas révéler si le compte existe)
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex,
            WebRequest request) {
        
        log.warn("Utilisateur non trouvé: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setError("Unauthorized");
        errorResponse.setMessage("Téléphone ou mot de passe incorrect");
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(errorResponse);
    }
    
    /**
     * Gestion des arguments invalides (ex: téléphone déjà existant)
     * Retourne 400 Bad Request ou 409 Conflict selon le cas
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {
        
        log.warn("Argument invalide: {}", ex.getMessage());
        
        // Si c'est un problème de doublon (téléphone/email existant), retourner 409 Conflict
        HttpStatus status = ex.getMessage().contains("existe déjà") 
            ? HttpStatus.CONFLICT 
            : HttpStatus.BAD_REQUEST;
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity
            .status(status)
            .body(errorResponse);
    }
    
    /**
     * Gestion des opérations non supportées (endpoints pas encore implémentés)
     * Retourne 501 Not Implemented
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(
            UnsupportedOperationException ex,
            WebRequest request) {
        
        log.info("Endpoint pas encore implémenté: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
        errorResponse.setError("Not Implemented");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {

        log.warn("Ressource introuvable: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setError("Not Found");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            WebRequest request) {

        log.warn("Erreur métier: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Business Error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }


    /**
     * Gestion de toutes les autres exceptions
     * Retourne 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        log.error("Erreur inattendue", ex);
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError("Internal Server Error");
        errorResponse.setMessage("Une erreur inattendue s'est produite: " + ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(OffsetDateTime.now());
        error.setStatus(403);
        error.setError("Forbidden");
        error.setMessage(ex.getMessage());
        return ResponseEntity.status(403).body(error);
    }




}
