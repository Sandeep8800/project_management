package com.nexuspms.governance.security;

import java.lang.annotation.*;

/**
 * API Design S6/S11: for endpoints marked "membership" (any role) rather than a
 * specific Permission -- e.g. reading the backlog/board. Still 404s a non-member
 * exactly like RequirePermission (not leaking project existence), it just skips
 * the permission-set check since read access requires no specific permission
 * beyond having an active ProjectMembership row.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireMembership {

    String projectIdParam() default "projectId";
}
