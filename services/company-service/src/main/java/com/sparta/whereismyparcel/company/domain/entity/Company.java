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
@Table(name = "p_companies", schema = "company_db")
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

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "business_number", nullable = false, unique = true, length = 30)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CompanyStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private Company(
            UUID hubId,
            CompanyType companyType,
            String companyName,
            String businessNumber,
            String managerName,
            String managerPhone,
            String zipCode,
            String address,
            String addressDetail
    ) {
        this.hubId = hubId;
        this.companyType = companyType;
        this.companyName = companyName;
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
            String companyName,
            String businessNumber,
            String managerName,
            String managerPhone,
            String zipCode,
            String address,
            String addressDetail
    ) {
        Company company = Company.builder()
                .hubId(hubId)
                .companyType(companyType)
                .companyName(companyName)
                .businessNumber(businessNumber)
                .managerName(managerName)
                .managerPhone(managerPhone)
                .zipCode(zipCode)
                .address(address)
                .addressDetail(addressDetail)
                .build();

        return company;
    }

    public void updateDetails(
            CompanyType companyType,
            String companyName,
            String managerName,
            String managerPhone,
            String zipCode,
            String address,
            String addressDetail,
            CompanyStatus status
    ) {
        this.companyType = companyType;
        this.companyName = companyName;
        this.managerName = managerName;
        this.managerPhone = managerPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.status = status;
    }

    public void delete(String userId) {
        super.softDelete(userId);
        this.status = CompanyStatus.INACTIVE;
    }
}
