package cl.duoc.skyrimemporium.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import cl.duoc.skyrimemporium.seguridad.ApiSecurityErrorHandler;
import cl.duoc.skyrimemporium.seguridad.BearerTokenFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("La autenticación se realiza mediante /api/v1/auth/login");
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenFilter tokenFilter,
            ApiSecurityErrorHandler errorHandler) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
                                "/api/v1/usuarios/registro", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
