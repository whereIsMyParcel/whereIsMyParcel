package com.sparta.whereismyparcel.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // X-Username 헤더 값(username)을 담으므로 String 타입 사용
    // SecurityContext가 비어있는 시점에 생성되는 엔티티를 고려해 nullable 제약 제외
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private String updatedBy;

    private LocalDateTime deletedAt;
    private String deletedBy;

    public void softDelete(String username) {
        if (isDeleted()) return; // 이미 삭제된 엔티티 재삭제 방지 (멱등성 보장)
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = username;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
