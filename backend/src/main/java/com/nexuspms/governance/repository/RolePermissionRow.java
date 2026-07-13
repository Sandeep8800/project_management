package com.nexuspms.governance.repository;

import com.nexuspms.governance.domain.Permission;
import com.nexuspms.governance.domain.Role;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/** Maps the role_permissions seed table (Database Design S5.3) -- composite PK (role, permission), read-only. */
@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionRow.Key.class)
public class RolePermissionRow {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Permission permission;

    protected RolePermissionRow() {
        // JPA
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }

    public static class Key implements Serializable {
        private Role role;
        private Permission permission;

        public Key() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return role == key.role && permission == key.permission;
        }

        @Override
        public int hashCode() {
            return Objects.hash(role, permission);
        }
    }
}
