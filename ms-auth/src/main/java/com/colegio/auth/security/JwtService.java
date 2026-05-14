package com.colegio.auth.security;

import com.colegio.auth.entity.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Emite y valida JWT firmados con RS256.
 * La clave privada firma; la clave publica valida (consumida por el API Gateway via JWKS).
 */
@Slf4j
@Service
public class JwtService {

    private final ResourceLoader resourceLoader;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public JwtService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws Exception {
        // Carga de claves desde classpath. En produccion deben venir de un secret manager.
        this.privateKey = loadPrivateKey();
        this.publicKey = loadPublicKey();
        log.info("Claves JWT cargadas correctamente");
    }

    public String generateToken(UserAccount user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);

        // Construccion del token con claims minimos: identificador, nombre y roles.
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(
                        "uid", user.getId(),
                        "name", user.getFullName(),
                        "roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
                ))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PrivateKey loadPrivateKey() throws Exception {
        // Lectura del archivo PEM en formato PKCS8 y decodificacion Base64.
        Resource res = resourceLoader.getResource(privateKeyPath);
        try (InputStream is = res.getInputStream()) {
            String pem = new String(is.readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        }
    }

    private PublicKey loadPublicKey() throws Exception {
        // Lectura del archivo PEM en formato X.509 para la clave publica.
        Resource res = resourceLoader.getResource(publicKeyPath);
        try (InputStream is = res.getInputStream()) {
            String pem = new String(is.readAllBytes())
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }
    }
}
