package com.sparta.whereismyparcel.user.domain.repository;

import com.sparta.whereismyparcel.user.domain.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByBusinessNumber(String businessNumber);

	Optional<User> findByBusinessNumber(String businessNumber);
}
