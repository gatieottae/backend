package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.api.me.CursorUtils;
import com.gatieottae.backend.api.me.dto.*;
import com.gatieottae.backend.common.exception.BadRequestException;
import com.gatieottae.backend.repository.group.MyGroupQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyGroupsService {

    private final MyGroupQueryRepository repo;
    private final Clock clock = Clock.systemDefaultZone(); // 필요시 Bean 주입으로 교체

    @Transactional(readOnly = true)
    public CursorPageResponse<MyGroupItemDto> getMyGroups(
            Long userId,
            String q,
            String statusParam,    // "before" | "during" | "after" | null
            String sortParam,      // "startAsc" | "startDesc" | "titleAsc"
            Integer sizeParam,     // default 20, max 50
            String cursor
    ){
        SortOption sort = SortOption.from(sortParam);
        int size = (sizeParam == null ? 20 : Math.max(1, Math.min(50, sizeParam)));

        LocalDate today = LocalDate.now(clock);
        String status = normalize(statusParam);

        // 커서 파싱
        LocalDate cursorStart = null;
        String cursorTitle = null;
        Long cursorId = null;
        if (cursor != null && !cursor.isBlank()){
            CursorUtils.Decoded d;
            try {
                d = CursorUtils.decode(cursor);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid cursor");
            }
            if (!d.sortParam().equalsIgnoreCase(sort.param)) {
                throw new BadRequestException("Cursor sort mismatch");
            }
            cursorId = d.id();
            if (sort == SortOption.TITLE_ASC) {
                cursorTitle = d.key();
            } else {
                cursorStart = "NULL".equals(d.key()) ? null : LocalDate.parse(d.key());
            }
        }

        // 조회
        List<Group> groups = switch (sort) {
            case START_ASC -> repo.findMyGroupsStartAsc(userId, q, status, today, cursorStart, cursorId, size);
            case START_DESC -> repo.findMyGroupsStartDesc(userId, q, status, today, cursorStart, cursorId, size);
            case TITLE_ASC -> repo.findMyGroupsTitleAsc(userId, q, status, today, cursorTitle, cursorId, size);
        };

        // 매핑
        List<MyGroupItemDto> items = groups.stream()
                .map(g -> MyGroupItemDto.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .destination(g.getDestination())
                        .startDate(g.getStartDate())
                        .endDate(g.getEndDate())
                        .status(calcStatus(today, g.getStartDate(), g.getEndDate()))
                        .build())
                .toList();

        // nextCursor
        String next = null;
        if (items.size() == size && !items.isEmpty()){
            MyGroupItemDto last = items.get(items.size()-1);
            if (sort == SortOption.TITLE_ASC) {
                next = CursorUtils.encodeTitle(last.getName(), last.getId());
            } else {
                String k = (last.getStartDate() == null ? "NULL" : last.getStartDate().toString());
                next = CursorUtils.encodeStart(sort, k, last.getId());
            }
        }

        return CursorPageResponse.of(items, next);
    }

    private static TripStatus calcStatus(LocalDate today, LocalDate start, LocalDate end){
        if (start == null && end == null) return TripStatus.BEFORE; // 보수적 처리
        LocalDate s = (start != null ? start : today);
        LocalDate e = (end != null ? end : s);
        if (!today.isBefore(s) && !today.isAfter(e)) return TripStatus.DURING;
        if (today.isAfter(e)) return TripStatus.AFTER;
        return TripStatus.BEFORE;
    }

    private static String normalize(String s){
        if (s == null) return null;
        s = s.trim().toLowerCase();
        return switch (s) {
            case "before", "during", "after" -> s;
            default -> null;
        };
    }
}