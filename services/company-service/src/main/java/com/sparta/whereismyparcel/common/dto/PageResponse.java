package com.sparta.whereismyparcel.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,        //  실제 데이터 리스트
        int pageNumber,         //  현재 페이지 번호
        int pageSize,           //  한 페이지당 사이즈
        long totalElements,     //  전체 데이터 개수
        int totalPages,         //  전체 페이지 수
        boolean isLast
) {
    // 💡 스프링 Page 객체를 집어넣으면 깔끔한 DTO로 싹 세팅해주는 정적 팩토리 메서드
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
