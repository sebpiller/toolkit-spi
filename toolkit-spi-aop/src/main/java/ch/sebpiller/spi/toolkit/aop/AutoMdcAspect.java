package ch.sebpiller.spi.toolkit.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Aspect that lazily set some values into the MDC at the beginning of a method and clears it at the end.
 * Useful to easily attach the username and the request id to each log statement.
 * Designed to be woven to controllers, but can be used everywhere.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 1st aspect of the chain
public class AutoMdcAspect extends AbstractBaseAspectDefinition {

    private static final Map<String, Supplier<String>> GLOBAL_MDC_VARIABLES = Map.of(
            "user", () -> {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                return authentication == null || authentication instanceof AnonymousAuthenticationToken ?
                        "**anonymous**" : authentication.getName();
            },
            "request-id", () -> UUID.randomUUID().toString()
    );

    @Pointcut("within(@AutoMdc *)")
    public void beanAnnotatedWithAutoMdc() {
    }

    @Around("beanAnnotatedWithAutoMdc()")
    public Object autoMdc(ProceedingJoinPoint pjp) throws Throwable {
        lazyPutGlobals();

        try {
            var annot = ((MethodSignature) pjp.getSignature()).getMethod().getParameterAnnotations();

            for (var i = 0; i < annot.length; i++) {
                for (var a : annot[i]) {
                    if (a instanceof AutoMdc.Mdc aa) {
                        MDC.put(aa.value(), String.valueOf(pjp.getArgs()[i]));
                    }
                }
            }

            return pjp.proceed();
        } finally {
            GLOBAL_MDC_VARIABLES.forEach((k, v) -> MDC.remove(k));
        }
    }

    private static void lazyPutGlobals() {
        for (var entry : GLOBAL_MDC_VARIABLES.entrySet()) {
            if (MDC.get(entry.getKey()) == null) {
                MDC.put(entry.getKey(), entry.getValue().get());
            }
        }
    }

}