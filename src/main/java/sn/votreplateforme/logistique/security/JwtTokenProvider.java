package sn.votreplateforme.logistique.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Provider JWT - Génération et validation des tokens
 * 
 * Un token JWT contient :
 * - L'identifiant de l'utilisateur (subject)
 * - La date d'expiration
 * - Une signature cryptographique
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;
    
    @Value("${jwt.refresh-expiration:604800000}") // 7 jours par défaut
    private long refreshExpirationMs;
    
    /**
     * Génère un token JWT pour un utilisateur authentifié
     * 
     * @param authentication L'authentification Spring Security
     * @return Token JWT sous forme de String
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userDetails.getUsername());
    }
    
    /**
     * Génère un token JWT à partir d'un nom d'utilisateur (téléphone)
     * 
     * @param telephone Téléphone de l'utilisateur
     * @return Token JWT
     */
    public String generateTokenFromUsername(String telephone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .setSubject(telephone)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Génère un refresh token (durée de vie plus longue)
     * 
     * @param telephone Téléphone de l'utilisateur
     * @return Refresh token JWT
     */
    public String generateRefreshToken(String telephone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .setSubject(telephone)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Extrait le téléphone (username) du token JWT
     * 
     * @param token Token JWT
     * @return Téléphone de l'utilisateur
     */
    public String getTelephoneFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getSubject();
    }
    
    /**
     * Valide un token JWT
     * 
     * @param token Token JWT à valider
     * @return true si le token est valide
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            
            return true;
            
        } catch (MalformedJwtException ex) {
            log.error("Token JWT malformé");
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expiré");
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT non supporté");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string est vide");
        } catch (Exception ex) {
            log.error("Erreur de validation du token JWT", ex);
        }
        
        return false;
    }
    
    /**
     * Extrait la date d'expiration du token
     * 
     * @param token Token JWT
     * @return Date d'expiration
     */
    public Date getExpirationDateFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getExpiration();
    }
    
    /**
     * Vérifie si le token est expiré
     * 
     * @param token Token JWT
     * @return true si expiré
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
