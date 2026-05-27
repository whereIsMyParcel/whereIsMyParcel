package com.sparta.whereismyparcel.aislack.domain.repository;

import com.sparta.whereismyparcel.aislack.domain.entity.SlackMessage;
import com.sparta.whereismyparcel.aislack.domain.entity.SlackStatus; // SlackStatus 임포트
import org.springframework.data.domain.Page; // Page 임포트
import org.springframework.data.domain.Pageable; // Pageable 임포트
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID> {
    Page<SlackMessage> findAll(Pageable pageable); // Pageable을 받는 findAll 추가
    Page<SlackMessage> findBySlackStatus(SlackStatus slackStatus, Pageable pageable); // Pageable을 받는 findBySlackStatus 추가
}
