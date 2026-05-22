package com.sparta.whereismyparcel.company.domain.repository;

import com.sparta.whereismyparcel.company.domain.entity.CompanyMember;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {

    Boolean existsByUserId(String userId);

    Optional<CompanyMember> findByCompanyMemberIdAndStatus(UUID companyMemberId, CompanyMemberStatus status);
}
