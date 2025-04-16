package ch.sebpiller.spi.toolkit.aop;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Mdc {


    /**
     * Tag a method parameter to be inserted into the MDC. It will be removed from the context after the method execution.
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Add {
        String value() default "";
    }

}