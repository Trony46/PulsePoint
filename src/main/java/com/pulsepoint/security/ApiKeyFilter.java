package com.pulsepoint.security;

import com.pulsepoint.model.Source;
import com.pulsepoint.repository.SourceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private final SourceRepository sourceRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only intercept ingest endpoints — everything else passes straight through
        if (!path.startsWith("/api/ingest/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Read the API key from the request header
        String apiKey = request.getHeader("X-Api-Key");

        // No key provided at all
        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing X-Api-Key header\"}");
            return;
        }

        // Extract the sourceId from the URL path
        // Path looks like: /api/ingest/1  or  /api/ingest/1/batch
        // We split on "/" and grab the part after "ingest"
        String[] parts = path.split("/");
        Long sourceId;
        try {
            // parts[0]="" parts[1]="api" parts[2]="ingest" parts[3]="1"
            sourceId = Long.parseLong(parts[3]);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid source ID in URL\"}");
            return;
        }

        // Look up which source owns this API key
        Optional<Source> sourceByKey = sourceRepository.findByApiKey(apiKey);

        // Key doesn't exist in our database at all
        if (sourceByKey.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid API key\"}");
            return;
        }

        // Key exists but belongs to a DIFFERENT source than the one in the URL
        // This stops source 2 from pushing data to source 1's endpoint
        if (!sourceByKey.get().getId().equals(sourceId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"API key does not belong to this source\"}");
            return;
        }

        // Key is valid and matches the source in the URL — let the request through
        filterChain.doFilter(request, response);
    }
}