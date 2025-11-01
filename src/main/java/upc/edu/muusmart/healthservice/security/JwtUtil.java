package upc.edu.muusmart.healthservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.function.Function;

/**
 * Utility component for generating and validating JSON Web Tokens (JWTs) in the
 * Animal Management Service.
 *
 * <p>For the sake of this university project, the secret key and expiration
 * time are defined as constants. In a real-world scenario, these values
 * should be externalized into configuration properties or environment
 * variables.</p>
 */
@Component
public class JwtUtil {

    // NOTE: In production, store this in configuration rather than hard-coding.
    private final String jwtSecret = "ReplaceThisSecretWithAStrongKeyForProduction";
    private final long jwtExpirationMs = 60 * 60 * 1000; // 1 hour

    /**
     * Extracts the username from the JWT token.
     *
     * @param token the JWT token
     * @return username (subject) stored in the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim using the provided resolver.
     *
     * @param token          the token
     * @param claimsResolver function to apply to the token's claims
     * @param <T>            type of claim
     * @return extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates the token by checking that it has not expired and can be parsed.
     *
     * @param token the JWT token
     * @return true if the token is well-formed and not expired
     */
    public boolean validateToken(String token) {
        try {
            // parse to ensure signature is valid and expiration is considered
            return !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

}