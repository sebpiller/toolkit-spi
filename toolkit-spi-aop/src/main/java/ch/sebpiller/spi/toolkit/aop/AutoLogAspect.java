package ch.sebpiller.spi.toolkit.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Aspect implementing the log of calls made to methods of a class.
 * Mainly useful for debugging and development purposes. Still, can be used on production context.
 * Very similar in concept to <a href="https://www.slf4j.org/api/org/slf4j/ext/XLogger.html">XLogger</a>,
 * but using AOP instead of imperative paradigm.
 */
@Aspect
@RequiredArgsConstructor
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AutoLogAspect extends AbstractBaseAspectDefinition {

    private static final String ARGS_HIDDEN = "-args hidden-";
    private static final String ARG_HIDDEN = "-value hidden-";

    private static final String LOG_ENTERING = ">> entering {}.{}({})";
    private static final String LOG_ENTERING_DEPRECATED = ">> DEPRECATED CALL !! entering {}.{}({})";
    private static final String LOG_EXITING = "<< exiting {}.{}({}) {}";
    private static final String LOG_EXCEPTION = "!! exception thrown by {}.{}({}): {} \"{}\" (root: {})";
    private static final String LOG_SLOW_CALL = "!! SLOW CALL DETECTED: {}.{} took {}ms to process";

    private final AutoLog.Configuration config;

    @Pointcut("within(@AutoLog *)")
    public void beanAnnotatedWithAutoLog() {
    }

    @Pointcut("beanAnnotatedWithAutoLog()")
    public void publicMethodInsideAClassMarkedWithAutoLog() {
    }

    @Around("publicMethodInsideAClassMarkedWithAutoLog()")
    public Object autoLog(ProceedingJoinPoint pjp) throws Throwable {
        var start = System.currentTimeMillis();
        var method = ((MethodSignature) pjp.getSignature()).getMethod();
        var clazz = method.getDeclaringClass();
        var logger = LoggerFactory.getLogger(clazz);

        var optautoLog = findAnnotation(clazz, method, AutoLog.class);

        if (optautoLog.isEmpty()) {
            log.warn("AutoLog broken ?? ...");
            return pjp.proceed();
        }

        var autoLog = optautoLog.get();

        Throwable error = null;
        Object result = null;

        try {
            doBeforeCall(pjp, autoLog, logger, clazz, method);
            result = pjp.proceed();
        } catch (Throwable t) {
            error = t;
            doCatchThrowable(pjp, autoLog, logger, clazz, method, t);
            throw t;
        } finally {
            doFinally(pjp, autoLog, logger, clazz, method, error, result, start);
        }

        return result;
    }

    private void doBeforeCall(ProceedingJoinPoint pjp, AutoLog autoLog, Logger logger, Class<?> clazz, Method method) {
        if (enabled(autoLog.entering(), config.isEntering()) && logger.isDebugEnabled()) {
            var deprecated = findAnnotation(clazz, method, Deprecated.class).isPresent();
            logger.debug(deprecated ? LOG_ENTERING_DEPRECATED : LOG_ENTERING, clazz.getSimpleName(), method.getName(), argsToString(pjp, autoLog));
        }
    }

    private boolean enabled(AutoLog.Enabled x, boolean defValue) {
        if (x == AutoLog.Enabled.AUTO) {
            return defValue;
        }

        return x == AutoLog.Enabled.YES;
    }

    private Object argsToString(ProceedingJoinPoint pjp, AutoLog autoLog) {
        if (!enabled(autoLog.printArgs(), config.isPrintArgs())) {
            return ARGS_HIDDEN;
        }

        var sb = new ArrayList<String>();
        var i = 0;
        for (var p : ((MethodSignature) pjp.getSignature()).getMethod().getParameters()) {
            sb.add(p.getName() + "=" + (p.getAnnotation(AutoLog.Ignored.class) == null ? StringUtils.left(String.valueOf(pjp.getArgs()[i]), 100) : ARG_HIDDEN));
            i++;
        }

        return String.join(", ", sb);
    }

    private void doCatchThrowable(ProceedingJoinPoint pjp, AutoLog autoLog, Logger logger, Class<?> clazz, Method method, Throwable error) {
        if (enabled(autoLog.exception(), config.isException()) && logger.isDebugEnabled()) {
            logger.debug(LOG_EXCEPTION, clazz.getSimpleName(), method.getName(), argsToString(pjp, autoLog), error.getClass().getName(), error.getMessage(), ExceptionUtils.getRootCause(error).getClass().getName());
            logger.trace("{}", error.getClass().getName(), error);
        }
    }

    private void doFinally(ProceedingJoinPoint pjp, AutoLog autoLog, Logger logger, Class<?> clazz, Method method, Throwable error, Object result, long start) {
        var timeTook = System.currentTimeMillis() - start;

        if (enabled(autoLog.exiting(), config.isExiting()) && logger.isDebugEnabled()) {
            debugExiting(pjp, autoLog, logger, clazz, method, error, result, timeTook);
        }

        warnSlowCallIfNeeded(autoLog, logger, clazz, method, timeTook);
    }

    private void debugExiting(ProceedingJoinPoint pjp, AutoLog autoLog, Logger logger, Class<?> clazz, Method method, Throwable error, Object result, long timeTook) {
        String s;
        if (error != null) {
            s = "(exception raised: " + error.getClass().getSimpleName() + " - " + error.getMessage() + ")";
        } else if (Void.TYPE.equals(method.getReturnType())) {
            s = "(void)";
        } else {
            if (method.getReturnType().getName().equals(SPRING_WEB_RESPONSE_ENTITY_CLASS)) {
                var res = (result == null ? "" : "non ") + "null object";
                s = "(returning " + (enabled(autoLog.printResult(), config.isPrintResult()) ? StringUtils.left(String.valueOf(result), 1000) : res) + ")";
            } else {
                var res = (result == null ? "" : "non ") + "null object";
                s = "(returning " + (enabled(autoLog.printResult(), config.isPrintResult()) ? StringUtils.left(String.valueOf(result), 1000) : res) + ")";
            }
        }

        if (enabled(autoLog.measureExecTime(), config.isMeasureExecTime())) {
            s += " - exec time: " + timeTook + " ms";
        }

        logger.debug(LOG_EXITING, clazz.getSimpleName(), method.getName(), argsToString(pjp, autoLog), s);
    }

    private void warnSlowCallIfNeeded(AutoLog autoLog, Logger logger, Class<?> clazz, Method method, long timeTook) {
        if (enabled(autoLog.warnSlowCalls(), config.isWarnSlowCalls()) && logger.isWarnEnabled() && (timeTook >= (autoLog.slowCallSeconds() * 1_000L))) {
            logger.warn(LOG_SLOW_CALL, clazz.getSimpleName(), method.getName(), timeTook);
        }
    }
}