package co.edu.unbosque.biohub.comun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("interna")
@ComponentScan("co.edu.unbosque.biohub.interna")
public class PerfilInternaConfig {

    // Cerrado por defecto. HU-12 agrega el login con JWT y los permisos por rol.
    @Bean
    SecurityFilterChain seguridadInterna(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}