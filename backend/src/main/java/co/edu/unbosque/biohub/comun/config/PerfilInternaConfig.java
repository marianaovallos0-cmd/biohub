package co.edu.unbosque.biohub.comun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import co.edu.unbosque.biohub.interna.seguridad.infraestructura.ConvertidorJwt;
import co.edu.unbosque.biohub.interna.seguridad.web.ManejadorAccesoDenegado;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@Profile("interna")
@EnableMethodSecurity
@ComponentScan("co.edu.unbosque.biohub.interna")
public class PerfilInternaConfig {

    @Bean
    SecurityFilterChain seguridadInterna(HttpSecurity http, ConvertidorJwt convertidorJwt,
                                        ManejadorAccesoDenegado accesoDenegado) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/interno/auth/login").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.accessDeniedHandler(accesoDenegado))
            .oauth2ResourceServer(oauth -> oauth
                .accessDeniedHandler(accesoDenegado)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt)));
        return http.build();
    }

    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}