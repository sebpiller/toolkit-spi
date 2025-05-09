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
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * Aspect for managing MDC (Mapped Diagnostic Context) variables in a controlled and automated way during
 * method execution. It integrates with Spring's Aspect-Oriented Programming (AOP) framework.
 *
 * <p>The primary responsibility of this aspect is to automatically populate and remove MDC variables
 * for logging purposes during the execution of methods annotated with {@code @Mdc}. It ensures that
 * MDC variables are put in context before method execution and cleared after the execution, maintaining
 * a clean state across threads and requests.
 *
 * <h3>Functionality:</h3>
 * <ul>
 *     <li>Defines a {@link Pointcut} that applies to beans annotated with {@code @Mdc}.</li>
 *     <li>Intercepts execution of annotated methods and manages the lifecycle of MDC variables using {@code @Around} advice.</li>
 *     <li>Automatically adds "global" MDC variables such as {@code user}, {@code request-id}, {@code class}, and {@code method}.</li>
 *     <li>Supports method parameters annotated with {@code @Mdc.Add} to add custom MDC entries for method arguments.</li>
 * </ul>
 *
 * <h3>Global MDC Variables:</h3>
 * <ul>
 *     <li><b>user</b>: The currently authenticated user's username.
 *         If the user is anonymous or unauthenticated, defaults to {@code "**anonymous**"}.</li>
 *     <li><b>request-id</b>: A unique identifier for each method invocation, generated using {@link UUID}.</li>
 *     <li><b>class</b>: The fully qualified name of the class containing the executed method.</li>
 *     <li><b>method</b>: The name of the executed method.</li>
 * </ul>
 *
 * <h3>Annotated Method Parameters:</h3>
 * Method parameters can be annotated with {@link Mdc.Add} to add their values to the MDC under a specific key.
 * By default, the key is derived from the parameter name unless overridden in the annotation.
 *
 * <h3>Lifecycle:</h3>
 * <ol>
 *     <li>MDC variables are lazily added globally using {@link #lazyPutGlobals(ProceedingJoinPoint)} before method execution.</li>
 *     <li>Custom MDC variables based on method arguments are added using {@link #putAnnotatedArgs(ProceedingJoinPoint)}.</li>
 *     <li>The method proceeds with its invocation within the {@code @Around} advice.</li>
 *     <li>Custom MDC variables are removed after execution using {@link #removeAnnotatedArgs(ProceedingJoinPoint)}.</li>
 *     <li>Global MDC variables are cleared after execution using {@link #removeGlobals()} to avoid residual data leakage.</li>
 * </ol>
 *
 * <h3>Constraints:</h3>
 * <ul>
 *     <li>This aspect applies only to beans annotated with {@code @Mdc}.</li>
 *     <li>All annotated parameters must either have a non-null value or handle {@code null} cases explicitly.</li>
 *     <li>The MDC structure relies on thread-local storage provided by SLF4J and Logback. Ensure that logback-core and slf4j-api dependencies are properly included in the project
 * .</li>
 * </ul>
 *
 * <h3>Thread-Safety:</h3>
 * This class and associated workflows are safe to use in multithreaded environments. MDC, by nature, is backed by
 * thread-local storage to manage per-thread context information.
 *
 * <h3>Order of Execution:</h3>
 * This is the highest-priority aspect in its AOP chain due to its {@link Order}
 * annotation with precedence set to {@code Ordered.HIGHEST_PRECEDENCE}.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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

    private void lazyPutGlobals(ProceedingJoinPoint pjp) {
        for (var entry : GLOBAL_MDC_VARIABLES.entrySet()) {
            if (MDC.get(entry.getKey()) == null) {
                MDC.put(entry.getKey(), entry.getValue().apply(pjp));
            }
        }
    }

    private void putAnnotatedArgs(ProceedingJoinPoint pjp) {
        doWithAnnotatedArg(pjp, (x, y) -> MDC.put(x, String.valueOf(y)));
    }

    private void removeAnnotatedArgs(ProceedingJoinPoint pjp) {
        doWithAnnotatedArg(pjp, (x, y) -> MDC.remove(x));
    }

    private static void removeGlobals() {
        GLOBAL_MDC_VARIABLES.forEach((k, v) -> MDC.remove(k));
    }

    private static void doWithAnnotatedArg(ProceedingJoinPoint pjp, BiConsumer<String, Object> x) {
        var method = ((MethodSignature) pjp.getSignature()).getMethod();
        var annot = method.getParameterAnnotations();

        for (var i = 0; i < annot.length; i++) {
            for (var a : annot[i]) {
                if (a instanceof Mdc.Add aa) {
                    x.accept("param_" + (aa.value().isBlank() ? method.getParameters()[i].getName() : aa.value()),
                            String.valueOf(pjp.getArgs()[i]));
                }
            }
        }
    }

}