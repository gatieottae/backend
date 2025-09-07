package com.gatieottae.backend.repository.schedule.view;

/** 참석자 샘플 표시에 필요한 최소 필드만 투사 */
public interface AttendeeSampleView {
    Long getMemberId();
    String getDisplayName(); // COALESCE(nickname, name)
}