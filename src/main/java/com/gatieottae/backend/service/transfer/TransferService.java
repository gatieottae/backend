package com.gatieottae.backend.service.transfer;

import com.gatieottae.backend.api.transfer.dto.*;
import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import com.gatieottae.backend.repository.expense.TransferRepository;
import com.gatieottae.backend.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TransferService
 *
 * 설계 포인트
 * - commitDrafts(): 멱등 처리. 진행중(REQUESTED/SENT) 중복 시 409 대신 기존 건을 반환.
 * - markSent/confirm/rollback: 상태 전이를 멱등하게 처리(이미 목표 상태면 그대로 반환).
 * - 검증: 금액>0, 자기 자신 송금 금지, 접근 권한 점검.
 * - 알림 훅(NotificationService): 보냈어요/확인/보채기 시점 호출 포인트 유지.
 *
 * 주의
 * - enum(TransferStatus)에 CANCELED/ROLLED_BACK이 없다면 본 파일은 참조하지 않습니다.
 *   롤백은 REQUESTED 로 되돌리는 방식으로 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final NotificationService notificationService; // Redis Pub/Sub or WebSocket publisher

    /**
     * 송금 초안 확정(배치 생성, 멱등).
     * - 동일 (groupId, from, to)에 대해 진행중(REQUESTED/SENT) 건이 있으면 기존 건 반환.
     */
    public List<TransferResponseDto> commitDrafts(TransferCommitRequestDto req) {
        Objects.requireNonNull(req, "request is null");
        Objects.requireNonNull(req.getGroupId(), "groupId is null");

        final Long groupId = req.getGroupId();
        final List<TransferCommitRequestDto.Item> items =
                req.getItems() != null ? req.getItems() : List.of();

        if (items.isEmpty()) return List.of();

        List<TransferResponseDto> result = new ArrayList<>(items.size());

        for (TransferCommitRequestDto.Item item : items) {
            validateDraftItem(item);

            // 진행중 중복 여부
            long dup = transferRepository.countByGroupIdAndFromMemberIdAndToMemberIdAndStatusIn(
                    groupId, item.getFromMemberId(), item.getToMemberId(),
                    List.of(TransferStatus.REQUESTED, TransferStatus.SENT)
            );

            if (dup > 0) {
                // 가장 최근 진행중 1건 반환
                Transfer existing = transferRepository
                        .findFirstByGroupIdAndFromMemberIdAndToMemberIdAndStatusInOrderByCreatedAtDesc(
                                groupId, item.getFromMemberId(), item.getToMemberId(),
                                List.of(TransferStatus.REQUESTED, TransferStatus.SENT)
                        )
                        .orElseThrow(() -> new IllegalStateException("진행중 건 집계와 조회가 불일치합니다."));
                result.add(TransferResponseDto.fromEntity(existing));
                continue;
            }

            // 신규 생성
            Transfer t = Transfer.builder()
                    .groupId(groupId)
                    .fromMemberId(item.getFromMemberId())
                    .toMemberId(item.getToMemberId())
                    .amount(item.getAmount())
                    .status(TransferStatus.REQUESTED)
                    .memo(item.getMemo())
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .expenseId(item.getExpenseId())
                    .build();

            Transfer saved = transferRepository.save(t);
            result.add(TransferResponseDto.fromEntity(saved));
        }

        return result;
    }

    /**
     * 보냈어요(멱등).
     * - from 본인만 가능
     * - REQUESTED → SENT
     * - 이미 SENT/CONFIRMED면 그대로 반환
     */
    public TransferResponseDto markSent(Long groupId, Long id, Long actorMemberId, TransferActionRequestDto body) {
        Transfer t = find(groupId, id);

        if (!Objects.equals(t.getFromMemberId(), actorMemberId)) {
            throw new SecurityException("보낸 사람만 '보냈어요' 처리할 수 있습니다.");
        }

        if (t.getStatus() == TransferStatus.SENT || t.getStatus() == TransferStatus.CONFIRMED) {
            return TransferResponseDto.fromEntity(t); // 멱등
        }
        if (t.getStatus() != TransferStatus.REQUESTED) {
            throw new IllegalStateException("현재 상태에서는 '보냈어요'로 변경할 수 없습니다.");
        }

        t.setStatus(TransferStatus.SENT);
        if (body != null && body.getMemo() != null) t.setMemo(body.getMemo());
        t.setUpdatedAt(OffsetDateTime.now());

        // 상대방에게 알림
        notificationService.notifySent(t.getToMemberId(), t.getId(), t.getAmount(), groupId, t.getFromMemberId());

        return TransferResponseDto.fromEntity(t);
    }

    /**
     * 받았어요(확인, 멱등).
     * - to 본인만 가능
     * - SENT → CONFIRMED
     * - 이미 CONFIRMED면 그대로 반환
     */
    public TransferResponseDto confirm(Long groupId, Long id, Long actorMemberId, TransferActionRequestDto body) {
        Transfer t = find(groupId, id);

        if (!Objects.equals(t.getToMemberId(), actorMemberId)) {
            throw new SecurityException("받는 사람만 '확인' 처리할 수 있습니다.");
        }

        if (t.getStatus() == TransferStatus.CONFIRMED) {
            return TransferResponseDto.fromEntity(t); // 멱등
        }
        if (t.getStatus() != TransferStatus.SENT) {
            throw new IllegalStateException("현재 상태에서는 '확인'으로 변경할 수 없습니다. (먼저 '보냈어요' 필요)");
        }

        t.setStatus(TransferStatus.CONFIRMED);
        if (body != null && body.getMemo() != null) t.setMemo(body.getMemo());
        t.setUpdatedAt(OffsetDateTime.now());

        // 송금자에게 알림
        notificationService.notifyConfirmed(t.getFromMemberId(), t.getId(), t.getAmount());

        return TransferResponseDto.fromEntity(t);
    }

    /**
     * 롤백.
     * - 보낸 사람(REQUESTED/SENT) 또는 관리자 가능
     * - CONFIRMED는 기본 불가(정책에 따라 관리자만 허용 가능)
     * - enum에 ROLLED_BACK이 없다면 REQUESTED로 되돌리는 정책으로 처리
     */
    public TransferResponseDto rollback(Long groupId, Long id, Long actorMemberId,
                                        TransferActionRequestDto body, boolean isAdmin) {
        Transfer t = find(groupId, id);

        boolean canSenderRollback =
                Objects.equals(t.getFromMemberId(), actorMemberId) &&
                        (t.getStatus() == TransferStatus.REQUESTED || t.getStatus() == TransferStatus.SENT);

        if (!(isAdmin || canSenderRollback)) {
            throw new SecurityException("롤백 권한이 없습니다.");
        }

        if (t.getStatus() == TransferStatus.CONFIRMED && !isAdmin) {
            throw new IllegalStateException("CONFIRMED 상태는 관리자만 롤백 가능합니다.");
        }

        // enum(ROLLED_BACK) 유무와 무관하게 일단 REQUESTED로 되돌리는 정책
        t.setStatus(TransferStatus.REQUESTED);
        if (body != null && body.getMemo() != null) t.setMemo(body.getMemo());
        t.setUpdatedAt(OffsetDateTime.now());

        // 알림 (양쪽 모두에게)
        notificationService.notifyRolledBack(List.of(t.getFromMemberId(), t.getToMemberId()), t.getId(), t.getAmount());

        return TransferResponseDto.fromEntity(t);
    }

    /**
     * 증빙 첨부/수정.
     * - 보낸 사람만 가능
     */
    public TransferResponseDto attachProof(Long groupId, Long id, Long actorMemberId, TransferProofRequestDto body) {
        Transfer t = find(groupId, id);

        if (!Objects.equals(t.getFromMemberId(), actorMemberId)) {
            throw new SecurityException("증빙은 보낸 사람만 첨부할 수 있습니다.");
        }
        if (body == null || (body.getProofUrl() == null && body.getMemo() == null)) {
            throw new IllegalArgumentException("첨부할 내용이 없습니다.");
        }

        if (body.getProofUrl() != null) t.setProofUrl(body.getProofUrl());
        if (body.getMemo() != null) t.setMemo(body.getMemo());
        t.setUpdatedAt(OffsetDateTime.now());

        // 수신자에게 증빙 첨부 알림
        notificationService.notifyProofAttached(t.getToMemberId(), t.getId(), t.getProofUrl());

        return TransferResponseDto.fromEntity(t);
    }

    /**
     * 보채기(알림).
     * - 관련 당사자(from 또는 to)만 가능
     */
    public void nudge(Long groupId, Long id, Long actorMemberId) {
        Transfer transfer = transferRepository.findFirstByIdAndGroupId(id, groupId)
                .orElseThrow(() -> new IllegalArgumentException("송금 내역을 찾을 수 없습니다."));

        if (!Objects.equals(transfer.getFromMemberId(), actorMemberId)
                && !Objects.equals(transfer.getToMemberId(), actorMemberId)) {
            throw new SecurityException("해당 송금에 권한이 없습니다.");
        }

        Long targetMemberId = Objects.equals(transfer.getFromMemberId(), actorMemberId)
                ? transfer.getToMemberId()
                : transfer.getFromMemberId();

        String message = String.format("📢 정산 보채기: %d원이 아직 처리되지 않았습니다.", transfer.getAmount());
        notificationService.sendNudge(targetMemberId, message);
    }

    /**
     * 나(멤버) 기준으로 해당 그룹에서 관련된 모든 송금 조회
     */
    @Transactional(readOnly = true)
    public List<TransferResponseDto> getTransfersForMember(Long groupId, Long memberId) {
        return transferRepository.findMyTransfersInGroup(groupId, memberId)
                .stream()
                .map(TransferResponseDto::fromEntity)
                .toList();
    }

    /* ===========================
     * 내부 유틸
     * =========================== */

    private void validateDraftItem(TransferCommitRequestDto.Item item) {
        if (item == null) throw new IllegalArgumentException("draft item is null");
        if (item.getFromMemberId() == null || item.getToMemberId() == null) {
            throw new IllegalArgumentException("from/to is required");
        }
        if (Objects.equals(item.getFromMemberId(), item.getToMemberId())) {
            throw new IllegalArgumentException("자기 자신에게 송금할 수 없습니다.");
        }
        if (item.getAmount() == null || item.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }

    private Transfer find(Long groupId, Long id) {
        return transferRepository.findFirstByIdAndGroupId(id, groupId)
                .orElseThrow(() -> new EntityNotFoundException("Transfer not found"));
    }
}