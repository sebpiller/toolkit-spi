package ch.sebpiller.spi.toolkit.aop;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoMdc {


    /**
     * Tag a method parameter to be inserted into the MDC. It will be removed from the context after the method execution.
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Mdc {
        String value() default "";
    }

}