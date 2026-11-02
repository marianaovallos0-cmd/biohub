package co.edu.unbosque.biohub.comun.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("publica")
@EnableCaching
@ComponentScan("co.edu.unbosque.biohub.publica")
public class PerfilPublicaConfig {

    // /publico es de solo lectura y sin login. /externo lo abre HU-16 con la validación de la apiKey.
    @Bean
    SecurityFilterChain seguridadPublica(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/publico/**").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}