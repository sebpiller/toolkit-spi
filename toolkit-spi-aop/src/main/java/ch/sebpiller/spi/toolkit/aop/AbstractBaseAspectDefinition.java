package ch.sebpiller.spi.toolkit.aop;

import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

abstract class AbstractBaseAspectDefinition {

    protected static final String SPRING_WEB_RESPONSE_ENTITY_CLASS = "org.springframework.http.ResponseEntity";
    protected static final String SPRING_WEB_HEADERS = "org.springframework.http.HttpHeaders";
    protected static final String SPRING_MULTI_VALUE_MAP = "org.springframework.util.MultiValueMap";
    protected static final String SPRING_HTTP_STATUS_CODE = "org.springframework.http.HttpStatusCode";

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    protected <T extends Annotation> T getAnnotation(Class<?> clazz, Method method, Class<T> annot) {
        return findAnnotation(clazz, method, annot)
                .orElseThrow(() -> new IllegalStateException("annotation not found"));
    }

    protected <T extends Annotation> Optional<T> findAnnotation(Class<?> clazz, Method method, Class<T> annot) {
        // Direct annotation are easy to find.
        var result = AnnotationUtils.findAnnotation(method, annot);
        if (result != null) {
            return Optional.of(result);
        }

        // Some non-inherited annotations (like @Deprecated) can only be found looking in super interfaces
        var cl = clazz;
        while (cl != Object.class) {
            for (var i : cl.getInterfaces()) {
                Method superMethod = null;
                try {
                    superMethod = i.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    // ignore
                }

                if (superMethod != null) {
                    result = AnnotationUtils.findAnnotation(superMethod, annot);

                    if (result != null) {
                        return Optional.of(result);
                    }
                }
            }
            cl = cl.getSuperclass();
        }

        // Will look for annotation at the class level, all the way up to Object
        return Optional.ofNullable(AnnotationUtils.findAnnotation(clazz, annot));
    }
}