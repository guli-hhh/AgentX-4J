package com.agentx4j.starter.annotation;

import com.agentx4j.starter.AgentX4JAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 AgentX-4J 自动配置。
 *
 * <p>标注在 Spring Boot 主类上，自动装配所有 x402 相关 Bean。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @SpringBootApplication
 * @EnableX402
 * public class MyAgentApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyAgentApplication.class, args);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AgentX4JAutoConfiguration.class)
public @interface EnableX402 {
}
