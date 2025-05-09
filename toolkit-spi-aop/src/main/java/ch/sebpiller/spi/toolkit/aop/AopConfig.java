package ch.sebpiller.spi.toolkit.aop;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class responsible for enabling and configuring AOP (Aspect-Oriented Programming)
 * functionality. It integrates AspectJ support, scans for components within the same package
 * and associates configuration related to automatic logging with the application context.
 *
 * <h2>Primary Responsibilities:</h2>
 * - Enables AspectJ's proxy-based AOP support in the application by leveraging Spring's {@link EnableAspectJAutoProxy}.
 * - Automatically scans for Spring components in the package of this class and its sub-packages using {@link ComponentScan}.
 * - Exposes configuration properties related to the {@link AutoLog} annotation for automatic method execution logging.
 *
 * <h2>AOP Integration:</h2>
 * By activating {@link EnableAspectJAutoProxy}, this class facilitates the creation of proxies
 * for annotated components, enabling the use of AspectJ-style annotations for cross-cutting concerns.
 *
 * <h2>Logging Configuration:</h2>
 * The {@link EnableConfigurationProperties(AutoLog.Configuration.class)} directive ensures that
 * the configuration of the {@link AutoLog} annotation can be customized using external configuration
 * properties under the prefix `toolkit.autolog`. This allows precise control over logging behavior
 * applied to methods.
 *
 * <h2>Key Features of AutoLog:</h2>
 * - Entry, exit, exception, and execution time logging.
 * - Configurable method argument and result logging.
 * - Support for warning on slow method calls based on a defined threshold.
 *
 * <h2>Constraints:</h2>
 * This configuration assumes the presence of correctly annotated methods and components and requires the
 * application's context to include the {@link AutoLog} feature to function as expected.
 */
@Configuration
@ComponentScan(basePackageClasses = AopConfig.class)
@EnableAspectJAutoProxy
@EnableConfigurationProperties(AutoLog.Configuration.class)
public class AopConfig {
}
