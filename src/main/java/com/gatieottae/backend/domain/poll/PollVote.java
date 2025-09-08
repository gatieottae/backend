package com.gatieottae.backend.domain.poll;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "poll_vote",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_poll_vote_poll_member", columnNames = {"poll_id", "member_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false, foreignKey = @ForeignKey(name = "poll_vote_poll_fk"))
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false, foreignKey = @ForeignKey(name = "poll_vote_option_fk"))
    private PollOption option;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "voted_at", nullable = false)
    private OffsetDateTime votedAt;

    @PrePersist
    void prePersist() {
        if (votedAt == null) votedAt = OffsetDateTime.now();
    }
}