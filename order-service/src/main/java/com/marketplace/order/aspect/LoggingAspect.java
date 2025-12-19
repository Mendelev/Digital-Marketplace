package com.marketplace.order.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect for logging method execution in service layer.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut for all methods in service package.
     */
    @Pointcut("execution(* com.marketplace.order.service..*(..))")
    public void serviceMethods() {
    }

    /**
     * Pointcut for all methods in controller package.
     */
    @Pointcut("execution(* com.marketplace.order.controller..*(..))")
    public void controllerMethods() {
    }

    /**
     * Log method execution time and parameters for service methods.
     */
    @Around("serviceMethods()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.debug("Executing service method: {} with args: {}", methodName, Arrays.toString(args));

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.debug("Service method {} executed in {}ms", methodName, executionTime);
            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Service method {} failed after {}ms: {}", methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Log incoming requests to controllers.
     */
    @Around("controllerMethods()")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        log.info("Incoming request: {}", methodName);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Request completed: {} in {}ms", methodName, executionTime);
            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Request failed: {} after {}ms: {}", methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}
