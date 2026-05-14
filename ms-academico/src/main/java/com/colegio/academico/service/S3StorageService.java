package com.colegio.academico.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

/**
 * Wrapper sobre el SDK de AWS S3 apuntando a MinIO en entornos locales.
 * El path-style es obligatorio para compatibilidad con MinIO.
 */
@Slf4j
@Service
public class S3StorageService {

    @Value("${s3.endpoint}") private String endpoint;
    @Value("${s3.region}")   private String region;
    @Value("${s3.access-key}") private String accessKey;
    @Value("${s3.secret-key}") private String secretKey;
    @Value("${s3.bucket}")   private String bucket;

    private S3Client client;

    @PostConstruct
    public void init() {
        // Construccion del cliente S3 con credenciales estaticas y endpoint custom.
        client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        ensureBucket();
    }

    private void ensureBucket() {
        // Crea el bucket si no existe (idempotente).
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (Exception ex) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket {} creado", bucket);
        }
    }

    @CircuitBreaker(name = "s3CB")
    public void upload(String key, byte[] content, String contentType) {
        // Subida del objeto. Protegido por circuit breaker ante fallos del backend de objetos.
        client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
        log.info("Subido a S3 bucket={} key={}", bucket, key);
    }
}
