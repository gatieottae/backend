package com.gatieottae.backend.service.transfer;

import com.gatieottae.backend.api.transfer.dto.*;
import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import com.gatieottae.backend.repository.expense.TransferRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    // TODO: MemberRepository, GroupMemberRepository 주입해서 존재/소속 검증 추가 가능
    // private final MemberRepository memberRepository;
    // private final TravelGroupMemberRepository groupMemberRepository;

    /** 초안 확정(배치 생성). 중복(동일 from→to, REQUESTED/SENT 미해결) 방지 */
    public List<TransferResponseDto> commitDrafts(TransferCommitRequestDto req) {
        // (선택) 멤버 존재/그룹 소속 검증
        // validateGroupMembers(req.getGroupId(), collect all memberIds);

        return req.getItems().stream().map(item -> {
            if (item.getAmount() == null || item.getAmount() <= 0)
                throw new IllegalArgumentException("amount must be > 0");

            long dup = transferRepository.countByGroupIdAndFromMemberIdAndToMemberIdAndStatusIn(
                    req.getGroupId(), item.getFromMemberId(), item.getToMemberId(),
                    List.of(TransferStatus.REQUESTED, TransferStatus.SENT));

            if (dup > 0) {
                throw new DataIntegrityViolationException("이미 진행중인 동일 송금 건이 있습니다.");
            }

            Transfer t = Transfer.builder()
                    .groupId(req.getGroupId())
                    .fromMemberId(item.getFromMemberId())
                    .toMemberId(item.getToMemberId())
                    .amount(item.getAmount())
                    .status(TransferStatus.REQUESTED)
                    .memo(item.getMemo())
                    .build();

            Transfer saved = transferRepository.save(t);

            // TODO: 이벤트 발행 (TRANSFER_REQUESTED)
            return toResponse(saved);
        }).toList();
    }

    public TransferResponseDto markSent(Long groupId, Long id, Long actorMemberId, TransferActionRequestDto body) {
        Transfer t = find(groupId, id);
        // 권한: 보내는 사람만
        if (!t.getFromMemberId().equals(actorMemberId)) {
            throw new SecurityException("보낸 사람만 '보냈어요' 처리할 수 있습니다.");
        }
        // 상태 전이 허용 범위
        if (t.getStatus() != TransferStatus.REQUESTED) {
            throw new IllegalStateException("현재 상태에서는 '보냈어요'로 변경할 수 없습니다.");
        }
        t.setStatus(TransferStatus.SENT);
        if (body != null && body.getMemo() != null) t.setMemo(body.getMemo());

        // TODO: 이벤트 발행 (TRANSFER_SENT)
        return toResponse(t);
    }

    public TransferResponseDto confirm(Long groupId, Long id, Long actorMemberId, TransferActionRequestDto body) {
        Transfer t = find(groupId, id);
        // 권한: 받는 사람만
        if (!t.getToMemberId().equals(actorMemberId)) {
            throw new SecurityException("받는 사람만 '확인' 처리할 수 있습니다.");
        }
        if (t.getStatus() != TransferStatus.SENT) {
            throw new IllegalStateException("현재 상태에서는 '확인'으로 변경할 수 없습니다.");
        }
        t.setStatus(TransferStatus.CONFIRMED);
        if (body != null && body.getMemo() != null) t.setMemo(body.getMemo());

        // TODO: 이벤트 발행 (TRANSFER_CONFIRMED)
        return toResponse(t);
    }

    public TransferResponseDto rollback(Long groupId, Long id, Long actorMemberId, TransferActionRequestDto body, boolean isAdmin) {
        Transfer t = find(groupId, id);
        // 정책: 보낸 사람(REQUESTED/SENT) 또는 관리자만 롤백 가능
        boolean canSenderRollback = t.getFromMemberId().equals(actorMemberId)
                && (t.getStatus() == TransferStatus.REQUESTED || t.getStatus() == TransferStatus.SENT);
        if (!(isAdmin || canSenderRollback)) {
            throw new SecurityException("롤백 권한이 없습니다.");
        }
        if (t.getStatus() == TransferStatus.CONFIRMED) {
            // 필요 시 추가 정책: CONFIRMED도 관리자만 롤백 가능
            if (!isAdmin) throw new IllegalStateException("CONFIRMED 상태는 관리자만 롤백 가능합니다.");
        }
        t.setStatus(TransferStatus.ROLLED_BACK);
        if (body != null && body.getMemo() != null) t.setMemo(body.getMemo());

        // TODO: 이벤트 발행 (TRANSFER_ROLLED_BACK)
        return toResponse(t);
    }

    public TransferResponseDto attachProof(Long groupId, Long id, Long actorMemberId, TransferProofRequestDto body) {
        Transfer t = find(groupId, id);
        // 권한: 보낸 사람만 증빙 첨부 가능(선택 정책)
        if (!t.getFromMemberId().equals(actorMemberId)) {
            throw new SecurityException("증빙은 보낸 사람만 첨부할 수 있습니다.");
        }
        t.setProofUrl(body.getProofUrl());
        if (body.getMemo() != null) t.setMemo(body.getMemo());
        return toResponse(t);
    }

    private Transfer find(Long groupId, Long id) {
        return transferRepository.findFirstByIdAndGroupId(id, groupId)
                .orElseThrow(() -> new EntityNotFoundException("Transfer not found"));
    }

    private TransferResponseDto toResponse(Transfer t) {
        return TransferResponseDto.builder()
                .id(t.getId())
                .groupId(t.getGroupId())
                .fromMemberId(t.getFromMemberId())
                .toMemberId(t.getToMemberId())
                .amount(t.getAmount())
                .status(t.getStatus())
                .proofUrl(t.getProofUrl())
                .memo(t.getMemo())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}