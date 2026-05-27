package com.sparta.whereismyparcel.company.domain.repository;

import com.sparta.whereismyparcel.company.domain.entity.CompanyMember;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {

    Boolean existsByUserId(UUID userId);

    Optional<CompanyMember> findByIdAndStatus(UUID companyMemberId, CompanyMemberStatus status);

    Page<CompanyMember> findByCompanyIdAndStatus(UUID companyId, CompanyMemberStatus status, Pageable pageable);
}
