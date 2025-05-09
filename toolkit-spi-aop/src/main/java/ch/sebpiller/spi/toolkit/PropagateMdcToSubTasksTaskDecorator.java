package ch.sebpiller.spi.toolkit;

import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class PropagateMdcToSubTasksTaskDecorator implements TaskDecorator {
    /**
     * Decorates a given {@link Runnable} to propagate the MDC (Mapped Diagnostic Context)
     * to any sub-tasks executed by the decorated runnable. Ensures that the MDC context
     * of the original thread is preserved during execution of the runnable.
     *
     * @param runnable the original {@link Runnable} to be decorated. Must not be null.
     * @return a new {@link Runnable} that propagates the MDC context during execution.
     */
    @Override @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
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
