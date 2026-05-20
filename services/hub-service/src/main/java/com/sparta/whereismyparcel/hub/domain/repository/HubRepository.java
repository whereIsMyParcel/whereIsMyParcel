package com.sparta.whereismyparcel.hub.domain.repository;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    //  Soft Delete를 고려하여 deleted_at이 null인 데이터만 조회
    Optional<Hub> findByHubIdAndDeletedAtIsNull(UUID hubId);

    List<Hub> findAllByDeletedAtIsNull();
    
    // 초기 데이터 로딩 시 중복 체크용
    boolean existsByNameAndDeletedAtIsNull(String name);
}
