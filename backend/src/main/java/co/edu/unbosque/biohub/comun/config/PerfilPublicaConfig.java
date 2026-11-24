package co.edu.unbosque.biohub.comun.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import co.edu.unbosque.biohub.publica.integracion.aplicacion.AutenticarConsumidor;
import co.edu.unbosque.biohub.publica.integracion.web.FiltroApiKey;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@Profile("publica")
@EnableCaching
@ComponentScan("co.edu.unbosque.biohub.publica")
public class PerfilPublicaConfig {

    @Bean
    SecurityFilterChain seguridadPublica(HttpSecurity http, AutenticarConsumidor autenticarConsumidor) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new FiltroApiKey(autenticarConsumidor), AuthorizationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/publico/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/externo/hallazgos", "/externo/hallazgos/*")
                    .hasAuthority(FiltroApiKey.AUTORIDAD)
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .accessDeniedHandler((peticion, respuesta, error) -> responder403(respuesta))
                .authenticationEntryPoint((peticion, respuesta, error) -> responder403(respuesta)));
        return http.build();
    }

    private static void responder403(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        respuesta.setContentType("application/problem+json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(
                "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Esta ruta no está habilitada.\"}");
    }
}