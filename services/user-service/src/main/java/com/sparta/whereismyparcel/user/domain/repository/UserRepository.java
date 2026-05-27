package com.sparta.whereismyparcel.user.domain.repository;

import com.sparta.whereismyparcel.user.domain.entity.User;
import com.sparta.whereismyparcel.user.domain.UserRole;
import com.sparta.whereismyparcel.user.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByBusinessNumber(String businessNumber);

	boolean existsBySlackId(String slackId);

	boolean existsBySlackIdAndUserIdNot(String slackId, UUID userId);

	Optional<User> findBySlackId(String slackId);

	Optional<User> findByBusinessNumber(String businessNumber);

	List<User> findAllByCompanyId(UUID companyId);

	@Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND (:status IS NULL OR u.status = :status)")
	Page<User> findAllByFilter(@Param("role") UserRole role, @Param("status") UserStatus status, Pageable pageable);
}
