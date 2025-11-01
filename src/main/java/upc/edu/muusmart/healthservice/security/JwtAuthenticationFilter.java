package upc.edu.muusmart.healthservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
// no UserDetailsService is used here; roles are extracted from the JWT claims
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that authenticates requests based on a JWT passed in the
 * Authorization header. If a valid token is present, it populates the
 * {@link SecurityContextHolder} so downstream handlers see the request as
 * authenticated.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String username = null;

        // Extract token from header if it starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // Malformed token or invalid signature; ignore and continue
                username = null;
            }
        }

        // Authenticate user if we have a username and no auth is set yet
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Validate token without user lookup
            if (jwtUtil.validateToken(jwt)) {
                // Extract roles or role claim if present. Supports both "roles" (list) and
                // "role" (single value) to be compatible with various token formats.
                java.util.List<String> roles = new java.util.ArrayList<>();
                try {
                    Object rolesClaim = jwtUtil.extractClaim(jwt, claims -> claims.get("roles"));
                    if (rolesClaim == null) {
                        // Some tokens may store a single role under the key "role"
                        rolesClaim = jwtUtil.extractClaim(jwt, claims -> claims.get("role"));
                    }
                    if (rolesClaim instanceof java.util.List<?> list) {
                        // roles claim provided as a list
                        for (Object obj : list) {
                            if (obj != null) {
                                roles.add(obj.toString());
                            }
                        }
                    } else if (rolesClaim instanceof String str) {
                        // roles claim provided as a comma-separated string or single value
                        for (String r : str.split(",")) {
                            String trimmed = r.trim();
                            if (!trimmed.isEmpty()) {
                                roles.add(trimmed);
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // If any error occurs during claim extraction, leave roles empty to avoid granting access erroneously
                }
                java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
                for (String role : roles) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
                }
                // Create authentication token using username and authorities
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}