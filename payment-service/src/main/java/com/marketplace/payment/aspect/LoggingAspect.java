package com.marketplace.payment.aspect;

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
    @Around("execution(* com.marketplace.payment.service.*.*(..))")
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
     * For payment service, mask card numbers and security codes if added later.
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

                    // Mask card number fields (for future use)
                    if (argString.contains("cardNumber=")) {
                        argString = argString.replaceAll("cardNumber=[^,\\)\\]]+", "cardNumber=****");
                    }

                    // Mask CVV fields (for future use)
                    if (argString.contains("cvv=")) {
                        argString = argString.replaceAll("cvv=[^,\\)\\]]+", "cvv=***");
                    }

                    // Mask security code fields
                    if (argString.contains("securityCode=")) {
                        argString = argString.replaceAll("securityCode=[^,\\)\\]]+", "securityCode=***");
                    }

                    return argString;
                })
                .toList()
                .toString();
    }
}
