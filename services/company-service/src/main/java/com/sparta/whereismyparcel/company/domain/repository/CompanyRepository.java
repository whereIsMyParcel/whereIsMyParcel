package com.sparta.whereismyparcel.company.domain.repository;

import com.sparta.whereismyparcel.company.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}
