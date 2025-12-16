package com.marketplace.auth.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logging aspect for service layer methods.
 * Logs method entry, exit, execution time, and sanitized arguments.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Around advice for all service methods.
     * Logs method execution details and timing.
     */
    @Around("execution(* com.marketplace.auth.service.*.*(..)) && !execution(* com.marketplace.auth.service.JwtService.getPublicKeyPem(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String operationName = joinPoint.getSignature().getName();
        
        // Store operation name in MDC for logging
        MDC.put("operation", operationName);
        
        // Sanitize arguments (remove sensitive data)
        Object[] args = joinPoint.getArgs();
        String sanitizedArgs = sanitizeArguments(args);
        
        log.debug("Executing method: {} with arguments: {}", methodName, sanitizedArgs);
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = false;
        
        try {
            result = joinPoint.proceed();
            success = true;
            return result;
            
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                log.debug("Method {} completed successfully in {} ms", methodName, executionTime);
            } else {
                log.debug("Method {} failed after {} ms", methodName, executionTime);
            }
            
            // Clean up MDC
            MDC.remove("operation");
        }
    }

    /**
     * Sanitize method arguments to remove sensitive data.
     * Replaces password fields and tokens with masked values.
     */
    private String sanitizeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }
                    
                    String argString = arg.toString();
                    
                    // Mask password fields
                    if (argString.contains("password=")) {
                        argString = argString.replaceAll("password=[^,\\)\\]]+", "password=***");
                    }
                    
                    // Mask passwordHash fields
                    if (argString.contains("passwordHash=")) {
                        argString = argString.replaceAll("passwordHash=[^,\\)\\]]+", "passwordHash=***");
                    }
                    
                    // Mask token fields
                    if (argString.contains("token=") && !argString.contains("tokenHash=")) {
                        argString = argString.replaceAll("token=[^,\\)\\]]+", "token=***");
                    }
                    
                    // Mask refreshToken fields
                    if (argString.contains("refreshToken=")) {
                        argString = argString.replaceAll("refreshToken=[^,\\)\\]]+", "refreshToken=***");
                    }
                    
                    // Mask tokenHash fields
                    if (argString.contains("tokenHash=")) {
                        argString = argString.replaceAll("tokenHash=[^,\\)\\]]+", "tokenHash=***");
                    }
                    
                    return argString;
                })
                .toList()
                .toString();
    }
}
