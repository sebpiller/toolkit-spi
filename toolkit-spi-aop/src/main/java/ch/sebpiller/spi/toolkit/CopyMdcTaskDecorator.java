package ch.sebpiller.spi.toolkit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class CopyMdcTaskDecorator implements TaskDecorator {
    @NotNull
    @Override
    public Runnable decorate(@NotNull Runnable runnable) {
        var contextMap = MDC.getCopyOfContextMap();

        return () -> {
            var oldContext = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                if (oldContext != null) {
                    MDC.setContextMap(oldContext);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
