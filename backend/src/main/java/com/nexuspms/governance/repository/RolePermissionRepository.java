package com.nexuspms.governance.repository;

import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Database Design S5.3: seed-only reference table, not admin-editable via the application in v1 (LLD S4.1). */
public interface RolePermissionRepository extends JpaRepository<RolePermissionRow, RolePermissionRow.Key> {

    @Query("select rp.permission from RolePermissionRow rp where rp.role = :role")
    List<Permission> findPermissionsByRole(@Param("role") Role role);
}
