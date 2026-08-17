package com.lawfirm.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByEnabledTrueOrderByRealNameAsc();

    Page<User> findByRole(User.Role role, Pageable pageable);

    Page<User> findByUsernameContainingOrRealNameContaining(String username, String realName, Pageable pageable);

    Page<User> findByUsernameContainingOrRealNameContainingAndRole(String username, String realName, User.Role role, Pageable pageable);

    long countByEnabledTrue();
}
