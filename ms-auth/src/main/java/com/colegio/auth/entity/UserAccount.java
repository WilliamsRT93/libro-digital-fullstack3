package com.colegio.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Set;

/**
 * Raiz de agregado para un usuario autenticado.
 * Los campos sensibles (password) se almacenan como hash BCrypt.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String fullName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 20)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    @Column(nullable = false)
    private boolean enabled = true;

    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
