package com.gatieottae.backend.scheduler;

import com.gatieottae.backend.service.poll.PollService;
import com.gatieottae.backend.repository.poll.PollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 매일 자정(KST) 기준으로 마감 대상 투표를 일괄 마감한다.
 * - 안전성을 위해 페이징으로 끊어 처리한다.
 * - 서버 지연/재시작 대비를 위해 보수 점검용 5분 간격 스케줄도 둔다(선택).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PollCloseScheduler {

    private static final int BATCH_SIZE = 500;

    private final PollRepository pollRepository;
    private final PollService pollService;

    private ZoneId zoneId = ZoneId.of("Asia/Seoul");

    /** 메인: 매일 00:00:00 KST */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void closeAtMidnight() {
        log.info("[PollCloseScheduler] start closeAtMidnight");
        runCloseJob();
        log.info("[PollCloseScheduler] end closeAtMidnight");
    }

    /** (선택) 보수: 5분마다 과거 마감 누락건이 있으면 회수 */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void sweepOverdue() {
        log.info("[PollCloseScheduler] start sweepOverdue");
        runCloseJob();
        log.info("[PollCloseScheduler] end sweepOverdue");
    }

    /** 실제 처리 루틴: 현재(now) 기준으로 마감 대상(OPEN & closesAt <= now)을 페이징으로 닫는다. */
    private void runCloseJob() {
        final OffsetDateTime now = OffsetDateTime.now(zoneId);
        int page = 0;
        int totalClosed = 0;

        while (true) {
            List<Long> ids = pollRepository.findIdsToClose(now, PageRequest.of(page, BATCH_SIZE));
            if (ids.isEmpty()) break;

            for (Long id : ids) {
                try {
                    // memberId는 '시스템 사용자' 개념으로 0 또는 null을 줄 수 있음.
                    // 현재 close()는 권한 체크가 없으니 0L로 호출해도 무방.
                    pollService.close(id, 0L);
                    totalClosed++;
                } catch (Exception e) {
                    // 한 건 실패해도 전체 작업은 계속 진행
                    log.warn("failed to close pollId={}", id, e);
                }
            }
            // 다음 페이지
            page++;
        }
        if (totalClosed > 0) {
            log.info("[PollCloseScheduler] closed polls: {}", totalClosed);
        } else {
            log.info("[PollCloseScheduler] no polls to close");
        }
    }
}