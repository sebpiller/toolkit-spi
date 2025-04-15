package ch.sebpiller.spi.toolkit.aop;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan(basePackageClasses = AopConfig.class)
@EnableAspectJAutoProxy
@EnableConfigurationProperties(AutoLog.Configuration.class)
public class AopConfig {
}
