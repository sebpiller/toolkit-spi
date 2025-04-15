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
 * Modify the headers to include X-Deprecated if the method it is applied on is actually deprecated and returns a
 * Spring's {@link org.springframework.http.ResponseEntity}. Uses reflection to avoid having a compile time
 * dependency to Spring Web from here. Silently ignored if applied to a wrong method signature, or if the call is
 * actually not deprecated.
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

                // Copy the originalheaders to the new one.
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