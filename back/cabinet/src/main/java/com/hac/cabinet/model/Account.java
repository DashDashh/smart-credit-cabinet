package com.hac.cabinet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "account")
public class Account implements UserDetails {
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  private UUID id;

  @Column(name = "login", length = 32, nullable = false, unique = true)
  private String login;

  @Column(name = "email", length = 254, nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", columnDefinition = "text", nullable = false)
  private String passwordHash;

  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 32, updatable = false, nullable = false)
  private Role role;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  @org.jspecify.annotations.Nullable
  public String getPassword() {
    return this.passwordHash;
  }

  @Override
  public String getUsername() {
    return this.login;
  }
}
