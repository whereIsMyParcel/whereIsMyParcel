package com.sparta.whereismyparcel.company.domain.repository;

import com.sparta.whereismyparcel.company.domain.entity.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {
}
