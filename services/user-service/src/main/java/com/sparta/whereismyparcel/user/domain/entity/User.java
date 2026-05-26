package com.sparta.whereismyparcel.user.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import com.sparta.whereismyparcel.user.domain.exception.InvalidApprovalStatusException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "p_users")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	// Keycloak sub claim 값을 그대로 사용 — @GeneratedValue 미사용
	@Id
	@Column(name = "user_id", columnDefinition = "uuid")
	private UUID userId;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "phone")
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status;

	@Column(name = "slack_id", unique = true)
	private String slackId;

	// COMPANY_MANAGER만 사용, 나머지 역할은 null
	@Column(name = "business_number", unique = true)
	private String businessNumber;

	// 게이트웨이 헤더에 X-Hub-Id/X-Company-Id가 없어 각 서비스가 Feign으로 조회하므로 여기서 관리
	@Column(name = "hub_id", columnDefinition = "uuid")
	private UUID hubId;

	@Column(name = "company_id", columnDefinition = "uuid")
	private UUID companyId;

	@Builder(access = AccessLevel.PRIVATE)
	private User(UUID userId, String username, String name, String email, String phone,
			UserRole role, UserStatus status, String slackId, String businessNumber,
			UUID hubId, UUID companyId) {
		this.userId = userId;
		this.username = username;
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.role = role;
		this.status = status;
		this.slackId = slackId;
		this.businessNumber = businessNumber;
		this.hubId = hubId;
		this.companyId = companyId;
	}

	public static User create(UUID userId, String username, String name, String email,
			String phone, UserRole role, String slackId, String businessNumber,
			UUID hubId, UUID companyId) {
		return User.builder()
				.userId(userId)
				.username(username)
				.name(name)
				.email(email)
				.phone(phone)
				.role(role)
				.status(UserStatus.PENDING)
				.slackId(slackId)
				.businessNumber(businessNumber)
				.hubId(hubId)
				.companyId(companyId)
				.build();
	}

	public void update(String name, String phone, String slackId) {
		if (name != null) this.name = name;
		if (phone != null) this.phone = phone;
		if (slackId != null) this.slackId = slackId;
	}

	public void approve() {
		if (this.status != UserStatus.PENDING) {
			throw new InvalidApprovalStatusException();
		}
		this.status = UserStatus.APPROVED;
	}

	public void reject() {
		if (this.status != UserStatus.PENDING) {
			throw new InvalidApprovalStatusException();
		}
		this.status = UserStatus.REJECTED;
	}
}
