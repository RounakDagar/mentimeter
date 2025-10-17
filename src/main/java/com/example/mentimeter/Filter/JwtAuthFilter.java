package com.example.mentimeter.Filter;

import com.example.mentimeter.Service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
// ... imports ...

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // 💡 NEW METHOD: Define which paths should NOT be filtered
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // The filter will be skipped if the path starts with /auth/ or /ws
        return path.startsWith("/auth/") || path.startsWith("/ws");
    }


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // When 'shouldNotFilter' returns true, this method is automatically skipped.

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. If we reach here, a token is expected. Check if the header is present and valid.
        // NOTE: The previous check for public endpoints is now in shouldNotFilter.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token found, but since this path wasn't excluded, it will likely lead to a 403.
            // Pass it on, and Spring Security will handle the unauthorized access.
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the JWT from the header
        // If the header is just "Bearer", the substring(7) will result in an empty string,
        // which will still lead to the MalformedJwtException in the JwtService.
        // It's safer to ensure the header is long enough.
        if (authHeader.length() < 7) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // 3. Extract the username from the token. This call can throw the MalformedJwtException.
        try {
            username = jwtService.extractUsername(jwt);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // Handle the error gracefully (e.g., log it and return 401/403 or just proceed)
            // For a robust system, you might want to return an explicit 401 here.
            System.err.println("Malformed JWT received: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        } catch (Exception e) {
            // Handle other JWT exceptions (e.g., SignatureException, ExpiredJwtException)
            System.err.println("JWT processing error: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }


        // 4. If username exists and user is not already authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load user details from the database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 5. If the token is valid, update the Security Context
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}