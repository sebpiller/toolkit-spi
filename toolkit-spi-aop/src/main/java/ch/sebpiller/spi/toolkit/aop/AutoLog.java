package ch.sebpiller.spi.toolkit.aop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.annotation.*;

/**
 * Annotation that enables automatic logging of method execution details, such as entry, exit,
 * exceptions, execution time, arguments, and results.
 *
 * <h3>Features:</h3>
 * - Configurable entry and exit logging.
 * - Optional exception logging.
 * - Measures and logs execution time.
 * - Optionally logs method arguments and result values.
 * - Warns about slow method executions based on configurable thresholds.
 *
 * <h3>Usage:</h3>
 * Can be applied where detailed logging for method execution is required, with support for
 * annotating methods or method parameters for additional context.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoLog {

    Enabled entering() default Enabled.AUTO;

    Enabled exiting() default Enabled.AUTO;

    Enabled exception() default Enabled.AUTO;

    Enabled measureExecTime() default Enabled.AUTO;

    Enabled printArgs() default Enabled.AUTO;

    Enabled printResult() default Enabled.AUTO;

    Enabled warnSlowCalls() default Enabled.AUTO;

    int slowCallSeconds() default 1;

    /**
     * Annotation to mark a method parameter to be ignored during logging when using the {@link AutoLog} annotation.
     * This indicates that the specific parameter should not be included in the logged arguments.
     *
     * Can be applied to method parameters where logging of argument details is enabled but specific parameters
     * should be excluded from being logged.
     *
     * Constraints:
     * - Applicable only to parameters of methods annotated with {@link AutoLog}.
     * - Ignored parameters will be excluded only from logging configurations that support argument logging.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Target({ElementType.PARAMETER})
    @interface Ignored {
    }

    enum Enabled {
        YES, NO, AUTO,;
    }

    /**
     * Configuration class for managing properties related to the AutoLog feature.
     * This configuration determines the behavior of automatic logging for methods,
     * including settings for logging entry, exit, exceptions, execution time, method arguments, and results.
     *
     * Fields:
     * - `enabled`: Enables or disables the AutoLog feature globally. Default is `false`.
     * - `entering`: If enabled, logs the entry of a method. Default is `true`.
     * - `exiting`: If enabled, logs the exit of a method. Default is `true`.
     * - `exception`: If enabled, logs exceptions thrown during method execution. Default is `true`.
     * - `measureExecTime`: If enabled, measures and logs method execution time. Default is `true`.
     * - `printArgs`: If enabled, logs the arguments passed to the method. Default is `false`.
     * - `printResult`: If enabled, logs the result returned by the method. Default is `false`.
     * - `warnSlowCalls`: If enabled, generates warnings for slow method executions based on the defined time threshold. Default is `true`.
     *
     * Constraints:
     * - This configuration is applied when the `AutoLog` feature is enabled in the application.
     * - Properties are mapped under the prefix "toolkit.autolog" in the application's configuration.
     */
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @ConfigurationProperties("toolkit.autolog")
    class Configuration {

        @Builder.Default
        private boolean enabled = false;

        @Builder.Default
        private boolean entering = true;

        @Builder.Default
        private boolean exiting = true;

        @Builder.Default
        private boolean exception = true;

        @Builder.Default
        private boolean measureExecTime = true;

        @Builder.Default
        private boolean printArgs = false;

        @Builder.Default
        private boolean printResult = false;

        @Builder.Default
        private boolean warnSlowCalls = true;

    }
}