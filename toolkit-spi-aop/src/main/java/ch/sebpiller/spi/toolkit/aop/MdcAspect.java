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
import java.util.function.Function;

/**
 * Aspect that lazily set some values into the MDC at the beginning of a method and clears it at the end.
 * Useful to easily attach the username and the request id to each log statement.
 * Designed to be woven to controllers, but can be used everywhere.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 1st aspect of the chain
public class MdcAspect extends AbstractBaseAspectDefinition {

    private static final Map<String, Function<ProceedingJoinPoint, String>> GLOBAL_MDC_VARIABLES = Map.of(
            "user", x -> {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                return authentication == null || authentication instanceof AnonymousAuthenticationToken ?
                        "**anonymous**" : authentication.getName();
            },
            "request-id", x -> UUID.randomUUID().toString(),
            "class", x -> x.getSignature().getDeclaringType().getName(),
            "method", x -> ((MethodSignature) x.getSignature()).getMethod().getName()
    );

    @Pointcut("within(@Mdc *)")
    public void beanAnnotatedWithMdc() {
    }

    @Around("beanAnnotatedWithMdc()")
    public Object autoMdc(ProceedingJoinPoint pjp) throws Throwable {
        lazyPutGlobals(pjp);
        putAnnotatedArgs(pjp);

        try {
            return pjp.proceed();
        } finally {
            removeAnnotatedArgs(pjp);
            removeGlobals();
        }
    }

    private static void putAnnotatedArgs(ProceedingJoinPoint pjp) {
        var method = ((MethodSignature) pjp.getSignature()).getMethod();
        var annot = method.getParameterAnnotations();

        for (var i = 0; i < annot.length; i++) {
            for (var a : annot[i]) {
                if (a instanceof Mdc.Add aa) {
                    MDC.put("param_" + (aa.value().isBlank() ?
                            method.getParameters()[i].getName() :
                            aa.value()), String.valueOf(pjp.getArgs()[i]));
                }
            }
        }
    }


    private void removeAnnotatedArgs(ProceedingJoinPoint pjp) {
        var method = ((MethodSignature) pjp.getSignature()).getMethod();
        var annot = method.getParameterAnnotations();

        for (var i = 0; i < annot.length; i++) {
            for (var a : annot[i]) {
                if (a instanceof Mdc.Add aa) {
                    MDC.remove("param_" + (aa.value().isBlank() ?
                            method.getParameters()[i].getName() :
                            aa.value()));
                }
            }
        }
    }

    private static void lazyPutGlobals(ProceedingJoinPoint pjp) {
        for (var entry : GLOBAL_MDC_VARIABLES.entrySet()) {
            if (MDC.get(entry.getKey()) == null) {
                MDC.put(entry.getKey(), entry.getValue().apply(pjp));
            }
        }
    }

    private static void removeGlobals() {
        GLOBAL_MDC_VARIABLES.forEach((k, v) -> MDC.remove(k));
    }

}