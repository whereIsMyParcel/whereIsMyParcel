package com.sparta.whereismyparcel.hub.domain.repository;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    // 초기 데이터 로딩 시 중복 체크용
    boolean existsByName(String name);
}
