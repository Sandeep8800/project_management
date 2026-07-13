package com.nexuspms.governance.security;

import com.nexuspms.common.exception.AuthorizationDeniedException;
import com.nexuspms.common.exception.ResourceNotFoundException;
import com.nexuspms.common.security.CurrentUser;
import com.nexuspms.governance.service.PermissionResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

/**
 * HLD S6 "AuthorizationInterceptor": resolves (caller, target project, requested
 * permission) before the annotated method body runs. Denial follows API Design
 * S11 -- a caller with no membership at all on the project gets 404 (not 403), so
 * project existence isn't leaked to non-members; a caller who IS a member but
 * lacks the specific permission gets 403.
 */
@Aspect
@Component
public class AuthorizationAspect {

    private final PermissionResolver permissionResolver;
    private final CurrentUser currentUser;

    public AuthorizationAspect(PermissionResolver permissionResolver, CurrentUser currentUser) {
        this.permissionResolver = permissionResolver;
        this.currentUser = currentUser;
    }

    @Around("@annotation(requirePermission)")
    public Object enforcePermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        UUID projectId = resolveProjectId(joinPoint, requirePermission.projectIdParam());
        UUID userId = currentUser.requireUserId();

        if (!permissionResolver.hasMembership(userId, projectId)) {
            throw new ResourceNotFoundException("Project " + projectId + " not found.");
        }
        if (!permissionResolver.hasPermission(userId, projectId, requirePermission.value())) {
            throw new AuthorizationDeniedException(
                    "You do not have the '" + requirePermission.value() + "' permission on this project.");
        }
        return joinPoint.proceed();
    }

    @Around("@annotation(requireMembership)")
    public Object enforceMembership(ProceedingJoinPoint joinPoint, RequireMembership requireMembership) throws Throwable {
        UUID projectId = resolveProjectId(joinPoint, requireMembership.projectIdParam());
        UUID userId = currentUser.requireUserId();

        if (!permissionResolver.hasMembership(userId, projectId)) {
            throw new ResourceNotFoundException("Project " + projectId + " not found.");
        }
        return joinPoint.proceed();
    }

    private UUID resolveProjectId(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            String candidateName = pathVariableName(parameters[i]);
            if (candidateName != null && candidateName.equals(paramName) && args[i] instanceof UUID uuid) {
                return uuid;
            }
        }
        throw new IllegalStateException(
                "Authorization annotation on " + method.getName() + " could not resolve @PathVariable '" + paramName + "'");
    }

    private String pathVariableName(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation instanceof PathVariable pv) {
                return pv.value().isEmpty() ? parameter.getName() : pv.value();
            }
        }
        return null;
    }
}
