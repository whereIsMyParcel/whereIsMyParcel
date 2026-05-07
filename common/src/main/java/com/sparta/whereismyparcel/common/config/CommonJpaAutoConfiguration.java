package com.sparta.whereismyparcel.common.config;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@AutoConfiguration
@ConditionalOnClass({EntityManager.class, EnableJpaAuditing.class})
@EnableJpaAuditing
public class CommonJpaAutoConfiguration {
}
