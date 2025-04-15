package ch.sebpiller.spi.toolkit.aop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoLog {

    Enabled entering() default Enabled.AUTO;

    Enabled exiting() default Enabled.AUTO;

    Enabled exception() default Enabled.AUTO;

    Enabled measureExecTime() default Enabled.AUTO;

    // printXxx defaults to 'false' to force explicit activation (can be too much data or sensitive information)
    Enabled printArgs() default Enabled.AUTO;

    Enabled printResult() default Enabled.AUTO;

    /**
     * Warns the developer in the log that a call took more time than it is acceptable.
     */
    Enabled warnSlowCalls() default Enabled.AUTO;

    /**
     * Min number of seconds took by a  call to be considered slow. No effect if {@link #warnSlowCalls()} is false.
     */
    int slowCallSeconds() default 1;

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Target({ElementType.PARAMETER})
    @interface Ignored {
    }

    enum Enabled {
        YES, NO, AUTO;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @ConfigurationProperties("toolkit.autolog")
    class Configuration {

        @Builder.Default
        private boolean entering = true;

        @Builder.Default
        private boolean  exiting  = true;

        @Builder.Default
        private boolean  exception = true;

        @Builder.Default
        private  boolean  measureExecTime = true;

        @Builder.Default
        private boolean  printArgs = false;

        @Builder.Default
        private boolean  printResult = false;

        @Builder.Default
        private boolean  warnSlowCalls = true;

    }
}