package com.waste2value.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste2value.auth.jwt.JwtAuthenticationFilter;
import com.waste2value.shared.ErrorResponse;
import com.waste2value.user.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = new ErrorResponse(
                    false,
                    HttpStatus.UNAUTHORIZED.value(),
                    "UNAUTHORIZED",
                    "Full authentication is required to access this resource",
                    request.getRequestURI(),
                    Instant.now(),
                    null
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = new ErrorResponse(
                    false,
                    HttpStatus.FORBIDDEN.value(),
                    "FORBIDDEN",
                    "You do not have permission to access this resource",
                    request.getRequestURI(),
                    Instant.now(),
                    null
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // Preflight OPTIONS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Public Category endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // Public Listing endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/{id:[0-9]+}").permitAll()

                        // Public Collector endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/collectors").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/collectors/{id:[0-9]+}").permitAll()

                        // Public Processor endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/processors").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/processors/{id:[0-9]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/processors/capabilities/**").permitAll()

                        // Public Review endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/user/**").permitAll()

                        // Public Image File serving
                        .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()

                        // Public Environmental Analytics & Transparency endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/analytics/trends").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/analytics/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/analytics/factors").permitAll()

                        // Role-specific Analytics endpoints
                        .requestMatchers("/api/v1/analytics/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/analytics/generator/**").hasRole("GENERATOR")
                        .requestMatchers("/api/v1/analytics/collector/**").hasRole("COLLECTOR")
                        .requestMatchers("/api/v1/analytics/processor/**").hasRole("PROCESSOR")

                        // Swagger / OpenAPI documentation
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()

                        // Collector route optimization endpoints
                        .requestMatchers("/api/v1/routes/**").hasRole("COLLECTOR")

                        // Generator listings endpoint (must precede generic rules)
                        .requestMatchers("/api/v1/listings/my").hasRole("GENERATOR")

                        // Admin category management
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // All other API endpoints require authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
