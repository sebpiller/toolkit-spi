package ch.sebpiller.spi.toolkit.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.util.ReflectionUtils.invokeMethod;


/**
 * Aspect that automatically adds a custom deprecation header to HTTP responses if the invoked public method
 * is marked as deprecated and returns a ResponseEntity. This behavior is only applied to classes or methods
 * annotated with the {@link AutoDeprecate} annotation.
 * <p>
 * The deprecation header name and message can be customized through the {@link AutoDeprecate} annotation's
 * properties: {@code header} and {@code message}.
 * <p>
 * Key functionalities:
 * 1. Identifies beans or methods annotated with {@link AutoDeprecate}.
 * 2. Intercepts public methods within these classes or methods.
 * 3. If the return type is {@code ResponseEntity} and the method is marked as deprecated (via {@code @Deprecated}),
 * adds a custom header to the response entity with the configured header name and message.
 * 4. Does not modify the response if the method or class is not annotated with {@link AutoDeprecate}, or if
 * the return type is not {@code ResponseEntity}.
 * <p>
 * Behavior Details:
 * - Uses the Spring AOP {@link Around} advice to wrap around the execution of public methods.
 * - Checks for the presence of {@code @Deprecated} on the method or its declaring class.
 * - Preserves the original headers in the ResponseEntity and adds the custom deprecation header.
 * - Logs a warning if the aspect cannot apply the deprecation header due to runtime issues.
 * <p>
 * This aspect is implemented generically without compile-time dependencies on Spring Web classes such
 * as {@code ResponseEntity}, {@code HttpHeaders}, or {@code HttpStatusCode}. These classes are dynamically
 * loaded and used via reflection to maintain compatibility.
 * <p>
 * Note:
 * - Annotation presence is validated across overridden methods in interfaces and superclasses.
 * - If the invoked method is not explicitly annotated with {@link AutoDeprecate}, the aspect
 * falls back to scanning its declaring class for the annotation.
 */
@Slf4j
@Aspect
@Component
public class AutoDeprecateAspect extends AbstractBaseAspectDefinition {

    @Pointcut("within(@AutoDeprecate *)")
    public void beanAnnotatedWithAutoDeprecate() {
    }

    @Pointcut("publicMethod() && beanAnnotatedWithAutoDeprecate()")
    public void publicMethodInsideAClassMarkedWithAutoDeprecate() {
    }

    @SuppressWarnings("unchecked")
    @Around("publicMethodInsideAClassMarkedWithAutoDeprecate()")
    public Object addDeprecatedHeaderToSpringResponseIfRequired(ProceedingJoinPoint pjp) throws Throwable {
        var method = ((MethodSignature) pjp.getSignature()).getMethod();
        var clazz = method.getDeclaringClass();

        var result = pjp.proceed();

        try {
            if (result != null &&
                    result.getClass().getName().equals(SPRING_WEB_RESPONSE_ENTITY_CLASS) && // no compile time dep to spring-web
                    findAnnotation(clazz, method, Deprecated.class).isPresent()) {

                var deprecatedAnnot = getAnnotation(clazz, method, AutoDeprecate.class);

                var springHttpHeaderClass = Class.forName(SPRING_WEB_HEADERS);
                var springMultiValueMapClass = Class.forName(SPRING_MULTI_VALUE_MAP);
                var responseEntityClass = Class.forName(SPRING_WEB_RESPONSE_ENTITY_CLASS);
                var httpStatusCodeClass = Class.forName(SPRING_HTTP_STATUS_CODE);

                var newHeaders = (Map<String, List<String>>) springHttpHeaderClass.getDeclaredConstructor().newInstance();

                // Copy the original headers to the new one.
                var originalHeaders = invokeMethod(result.getClass().getMethod("getHeaders"), result);
                if (originalHeaders != null) {
                    var entrySet = springHttpHeaderClass.getMethod("entrySet");

                    var headersSet = (Set<Map.Entry<String, List<String>>>) invokeMethod(entrySet, originalHeaders);
                    assert headersSet != null;

                    for (var e : headersSet) {
                        newHeaders.put(e.getKey(), e.getValue());
                    }
                }

                // Add the deprecated header.
                newHeaders.put(deprecatedAnnot.header(), List.of(deprecatedAnnot.message()));

                // Rebuild a ResponseEntity with the original body and the new headers.
                return responseEntityClass
                        .getConstructor(Object.class, springMultiValueMapClass, int.class)
                        .newInstance(
                                invokeMethod(responseEntityClass.getMethod("getBody"), result),
                                newHeaders,
                                invokeMethod(httpStatusCodeClass.getMethod("value"),
                                        invokeMethod(responseEntityClass.getMethod("getStatusCode"), result))
                        );
            }
        } catch (Exception e) {
            log.warn("aspect failed to apply Deprecated header to response", e);
        }

        return result;
    }
}