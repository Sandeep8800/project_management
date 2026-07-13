package com.nexuspms.identity.repository;

import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    boolean existsByEmployeeId(String employeeId);

    @Query("""
            select u from User u
            where (:status is null or u.status = :status)
              and (:department is null or u.department = :department)
              and (:q is null or lower(u.name) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%')))
            """)
    Page<User> search(@Param("status") UserStatus status, @Param("department") String department, @Param("q") String q, Pageable pageable);
}
