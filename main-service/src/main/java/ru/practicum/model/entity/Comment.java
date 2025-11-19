package ru.practicum.model.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.model.enums.CommentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommentStatus status = CommentStatus.PENDING;

    @Column(name = "created_date")
    private LocalDateTime created;

    @Column(name = "updated_date")
    private LocalDateTime updated;

    @Column(name = "edit_count")
    @Builder.Default
    private Integer editCount = 0;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @PrePersist
    protected void onCreate() {
        created = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updated = LocalDateTime.now();
    }
}