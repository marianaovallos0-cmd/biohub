package co.edu.unbosque.biohub.comun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("analitica")
@EnableScheduling
@ComponentScan("co.edu.unbosque.biohub.analitica")
public class PerfilAnaliticaConfig {

    // La analítica no tiene API. Solo responde la revisión de salud para Docker.
    @Bean
    SecurityFilterChain seguridadAnalitica(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
            .anyRequest().denyAll());
        return http.build();
    }
}