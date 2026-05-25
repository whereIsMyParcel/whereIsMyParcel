package com.sparta.whereismyparcel.aislack.domain.repository;

import com.sparta.whereismyparcel.aislack.domain.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID> {

}
