package ch.sebpiller.spi.toolkit.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect that set some values into the MDC at the beginning of a request, and clears it at the end.
 * Useful to easily attach the username and the request id to each log statement.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 1st aspect of the chain
public class AutoMdcAspect extends AbstractBaseAspectDefinition {

    @Pointcut("within(@AutoMdc *)")
    public void beanAnnotatedWithAutoMdc() {
    }

    @Pointcut("publicMethod() && beanAnnotatedWithAutoMdc()")
    public void publicMethodInsideAClassMarkedWithAutoMdc() {
    }

    @Around("publicMethodInsideAClassMarkedWithAutoMdc()")
    public Object autoMdc(ProceedingJoinPoint pjp) throws Throwable {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        MDC.put("user", authentication == null || authentication instanceof AnonymousAuthenticationToken ?
                "not authenticated" :
                authentication.getName()
        );
        MDC.put("request-id", UUID.randomUUID().toString());

        try {
            return pjp.proceed();
        } finally {
            MDC.clear();
        }
    }

}