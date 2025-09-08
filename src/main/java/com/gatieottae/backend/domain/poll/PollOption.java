package com.gatieottae.backend.domain.poll;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "poll_option", schema = "gatieottae",
        uniqueConstraints = @UniqueConstraint(name="uq_option_poll_content", columnNames={"poll_id","content"}))
public class PollOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="poll_id", nullable=false)
    private Poll poll;

    @Column(nullable=false, length=200)
    private String content;

    @Column(name="sort_order", nullable=false)
    private Integer sortOrder;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
}