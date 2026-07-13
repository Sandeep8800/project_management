package com.nexuspms.governance.security;

import com.nexuspms.governance.domain.Permission;

import java.lang.annotation.*;

/**
 * HLD S6: the single, shared enforcement point every module's mutating controller
 * methods rely on -- no module implements its own ad-hoc authorization logic.
 * Apply to a controller method with a {@code projectId} path variable (or use
 * {@link #projectIdParam()} to name a different one); AuthorizationAspect resolves
 * the caller's permission set for that project and denies before the method body
 * runs.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    Permission value();

    String projectIdParam() default "projectId";
}
