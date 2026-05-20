package com.sparta.whereismyparcel.company.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_company_members")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "companyMember_id",nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private CompanyRole role;

    @Builder(access = AccessLevel.PRIVATE)
    private CompanyMember(UUID userId, Company company, CompanyRole role) {
        this.userId = userId;
        this.company = company;
        this.role = role;
    }

    public static CompanyMember addMember(UUID userId, Company company, CompanyRole role) {
        CompanyMember companyMember = new CompanyMember(userId, company, role);
        company.addMember(companyMember);
        return companyMember;
    }

    public void updateDetails(CompanyRole role) {
        this.role = role;
    }

    public void delete(String userId) {
        super.softDelete(userId);
    }
}
