package ch.sebpiller.spi.toolkit.aop;

import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Base abstract class that provides reusable functionalities for Aspect-Oriented Programming (AOP).
 * This class defines common utilities for handling method-level pointcuts, annotations,
 * and their lookup mechanisms in a reflective and systematic manner.
 *
 * <p>Primary responsibilities of this base class include:
 * <ul>
 *     <li>Defining pointcuts that match all public methods execution.</li>
 *     <li>Providing utility methods to retrieve annotations from methods or classes,
 *         including handling inherited and interface-level annotations.</li>
 * </ul>
 *
 * <p>Subclasses of this abstract class can use these functionalities to implement
 * specific aspect logic such as logging, MDC management, or adding HTTP headers dynamically.
 *
 * <p>Known Subclasses:
 * <ul>
 *     <li>MdcAspect: Manages MDC variables for logging enhancements.</li>
 *     <li>AutoDeprecateAspect: Adds deprecation-related behavior to specific response objects.</li>
 *     <li>AutoLogAspect: Provides automatic logging mechanisms before and after method executions.</li>
 * </ul>
 *
 * <h3>Key Methods:</h3>
 * <ul>
 *     <li>{@link #publicMethod()}: A pointcut that matches all public methods.</li>
 *     <li>{@link #getAnnotation(Class, Method, Class)}: Retrieves a required annotation from a method or its class hierarchy.
 *         Throws an exception if the annotation is not found.</li>
 *     <li>{@link #findAnnotation(Class, Method, Class)}: Searches for an annotation within the provided method or class,
 *         including their interfaces and inherited types. Returns an {@code Optional} containing the annotation, if found.</li>
 * </ul>
 *
 * <p>Validation Constraints:
 * <ul>
 *     <li>All annotation lookups assume the provided class and method references are not null.</li>
 *     <li>Methods may throw {@link IllegalStateException} if a required annotation is absent where expected.</li>
 * </ul>
 *
 * <h3>Annotation Support:</h3>
 * The utility methods in this class operate on standard Java annotations and can detect annotations
 * directly declared on methods, inherited from interfaces, or present at class levels.
 *
 * <h3>Thread-Safety:</h3>
 * This class does not maintain any state; therefore, it is thread-safe. Subclasses should ensure
 * thread-safety in their own implementations.
 *
 * <h3>Extensibility:</h3>
 * Subclasses can define their own pointcuts and around, before, or after advices using the pointcut {@link #publicMethod()}
 * as a baseline or combining it with additional conditions.
 */
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

    /**
     * Finds the specified annotation on the given method or its inherited equivalents if applicable.
     * It looks for the annotation on the method provided, and if not directly found, it searches
     * for the annotation in superinterfaces and class hierarchy. If still not found, the method
     * searches for the annotation at the class level.
     *
     * <p>Constraints:
     * - The method parameter should represent the method to inspect.
     * - The clazz parameter should represent the class to which the method belongs.
     * - Only annotations of type {@link Annotation} and its subtypes can be processed.
     * - The search will check direct annotations, superinterface methods, and the class hierarchy.
     *
     * @param <T>   The type of annotation being searched for.
     * @param clazz The class where the method is declared or inherited. Must not be {@code null}.
     * @param method The method to inspect for annotations. Must not be {@code null}.
     * @param annot  The class of the annotation to search for. Must not be {@code null}.
     * @return An {@link Optional} containing the annotation if found, or {@link Optional#empty()} if not found.
     */
    protected <T extends Annotation> Optional<T> findAnnotation(Class<?> clazz, Method method, Class<T> annot) {
        // Direct annotations are straightforward to find.
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