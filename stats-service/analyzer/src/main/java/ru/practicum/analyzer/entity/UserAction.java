package ru.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_actions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_event", columnNames = {"user_id", "event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(name = "max_weight", nullable = false)
    Double maxWeight;

    @Column(name = "timestamp", nullable = false)
    java.time.Instant timestamp;
}