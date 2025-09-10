package com.gatieottae.backend.domain.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_message", schema = "gatieottae")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(length = 16, nullable = false)
    @Builder.Default
    private String type = "NORMAL";

    /** ✅ jsonb 매핑: mentions = [1,2,3] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.List<Long> mentions;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, columnDefinition = "timestamptz")
    private java.time.Instant sentAt;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private java.time.Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @PrePersist
    void prePersist() {
        if (type == null) type = "NORMAL";
    }
}