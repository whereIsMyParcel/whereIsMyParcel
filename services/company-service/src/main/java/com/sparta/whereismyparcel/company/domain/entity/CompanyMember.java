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
@Table(name = "p_company_members", schema = "company_db")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_member_id",nullable = false, updatable = false)
    private UUID companyMemberId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CompanyMemberStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private CompanyMember(
            String userId,
            Company company
    ) {
        this.userId = userId;
        this.company = company;
        this.status = CompanyMemberStatus.ACTIVE;
    }

    public static CompanyMember addMember(
            String userId,
            Company company
    ) {
        CompanyMember companyMember = CompanyMember.builder()
                .userId(userId)
                .company(company)
                .build();

        return companyMember;
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.status = CompanyMemberStatus.INACTIVE;
    }
}
