package com.lawfirm.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 启用且非隐藏的用户（人员下拉用） */
    @Query("select u from User u where u.enabled = true and (u.hidden = false or u.hidden is null) order by u.realName asc")
    List<User> findByEnabledTrueOrderByRealNameAsc();

    @Query("select u from User u where (u.hidden = false or u.hidden is null) and u.role = :role")
    Page<User> findVisibleByRole(@Param("role") User.Role role, Pageable pageable);

    @Query("select u from User u where (u.hidden = false or u.hidden is null) and (u.username like concat('%', :kw, '%') or u.realName like concat('%', :kw, '%'))")
    Page<User> findVisibleByKeyword(@Param("kw") String kw, Pageable pageable);

    @Query("select u from User u where (u.hidden = false or u.hidden is null) and (u.username like concat('%', :kw, '%') or u.realName like concat('%', :kw, '%')) and u.role = :role")
    Page<User> findVisibleByKeywordAndRole(@Param("kw") String kw, @Param("role") User.Role role, Pageable pageable);

    @Query("select u from User u where (u.hidden = false or u.hidden is null)")
    Page<User> findVisible(Pageable pageable);

    long countByEnabledTrue();
}
