package co.edu.unbosque.biohub.interna.seguridad.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.GeneradorToken;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.TokenEmitido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// Firma un JWT con HS256. Solo lleva el id del usuario y cuándo se vence
@Component
public class GeneradorTokenJwt implements GeneradorToken {

    private final JwtEncoder encoder;
    private final Duration duracion;

    public GeneradorTokenJwt(JwtEncoder encoder, @Value("${biohub.jwt.duracion:1h}") Duration duracion) {
        this.encoder = encoder;
        this.duracion = duracion;
    }

    @Override
    public TokenEmitido generar(UUID usuarioId) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(duracion);
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("biohub")
                .subject(usuarioId.toString())
                .issuedAt(ahora)
                .expiresAt(vence)
                .build();
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(encabezado, datos)).getTokenValue();
        return new TokenEmitido(token, vence);
    }
}