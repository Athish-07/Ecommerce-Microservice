package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bootstrap_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BootstrapState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bootstrapKey;

    @Column(nullable = false)
    private LocalDateTime completedAt;
}
