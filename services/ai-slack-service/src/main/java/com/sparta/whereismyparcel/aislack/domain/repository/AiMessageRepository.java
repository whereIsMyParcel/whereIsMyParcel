package com.sparta.whereismyparcel.aislack.domain.repository;

import com.sparta.whereismyparcel.aislack.domain.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
}
