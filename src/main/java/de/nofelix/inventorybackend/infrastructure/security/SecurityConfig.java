package de.nofelix.inventorybackend.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/**
 * Spring Security configuration for reactive WebFlux.
 *
 * <p>Configures JWT-based stateless authentication with role-based access control.</p>
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF for stateless API
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                
                // Disable form login and HTTP Basic
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                
                // Stateless session management
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                
                // Add JWT filter
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                
                // Configure exception handling
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return Mono.empty();
                        })
                        .accessDeniedHandler((exchange, denied) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return Mono.empty();
                        })
                )
                
                // Configure authorization rules
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers("/actuator/info").permitAll()
                        .pathMatchers("/actuator/prometheus").permitAll()
                        .pathMatchers("/h2-console/**").permitAll()
                        
                        // Swagger/OpenAPI endpoints
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        
                        // Read operations - allow USER and ADMIN
                        .pathMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("USER", "ADMIN")
                        
                        // Write operations - require ADMIN
                        .pathMatchers(HttpMethod.POST, "/api/v1/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                        
                        // All other actuator endpoints require ADMIN
                        .pathMatchers("/actuator/**").hasRole("ADMIN")
                        
                        // Deny all other requests
                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
