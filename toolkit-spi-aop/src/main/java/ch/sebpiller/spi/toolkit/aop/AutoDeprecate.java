package ch.sebpiller.spi.toolkit.aop;


import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * When applied around a method that returns ResponseEntity, add the X-Deprecated custom header to that response if the
 * method is marked as deprecated. Does nothing if not around a ResponseEntity.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoDeprecate {

    String header() default "X-Deprecated";

    String message() default "This call is deprecated, you should not continue to use it";

}