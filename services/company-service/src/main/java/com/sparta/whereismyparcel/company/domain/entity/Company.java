package com.sparta.whereismyparcel.company.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_companies")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", nullable = false,  updatable = false)
    private UUID id;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 30)
    private CompanyType companyType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "business_number", nullable = false, length = 30)
    private String businessNumber;

    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    @Column(name = "manager_phone", nullable = false, length = 50)
    private String managerPhone;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    private List<CompanyMember> companyMembers = new ArrayList<>();

    // 위도 경도 추후 추가 예정

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CompanyStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private Company(
            UUID hubId,
            CompanyType companyType,
            String name,
            String businessNumber,
            String managerName,
            String managerPhone,
            String zipCode,
            String address,
            String addressDetail
    ) {
        this.hubId = hubId;
        this.companyType = companyType;
        this.name = name;
        this.businessNumber = businessNumber;
        this.managerName = managerName;
        this.managerPhone = managerPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.status = CompanyStatus.ACTIVE;
    }

    public static Company create(
            UUID hubId,
            CompanyType companyType,
            String name,
            String businessNumber,
            String managerName,
            String managerPhone,
            String zipCode,
            String address,
            String addressDetail,
            UUID managerUserId
    ) {
        Company company = Company.builder()
                .hubId(hubId)
                .companyType(companyType)
                .name(name)
                .businessNumber(businessNumber)
                .managerName(managerName)
                .managerPhone(managerPhone)
                .zipCode(zipCode)
                .address(address)
                .addressDetail(addressDetail)
                .build();

        CompanyMember.addMember(managerUserId, company, CompanyRole.MANAGER);

        return company;
    }

    public void updateDetails(
            CompanyType companyType,
            String name,
            String managerName,
            String managerPhone,
            String zipCode,
            String address,
            String addressDetail,
            CompanyStatus status
    ) {
        this.companyType = companyType;
        this.name = name;
        this.managerName = managerName;
        this.managerPhone = managerPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.status = status;
    }

    public void addMember(CompanyMember companyMember) {
        this.companyMembers.add(companyMember);
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.status = CompanyStatus.INACTIVE;

        if (this.companyMembers != null) {
            this.companyMembers.forEach(companyMember -> {companyMember.delete(userId);});
        }
    }
}
