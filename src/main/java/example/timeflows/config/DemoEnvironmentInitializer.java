package example.timeflows.config;

import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Applies a complete, self-contained local environment when demo data is enabled. */
public class DemoEnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String PROPERTY_SOURCE_NAME = "timeflowsDemoEnvironment";

    private static final Map<String, Object> DEMO_PROPERTIES =
            Map.ofEntries(
                    Map.entry("spring.application.name", "timeflows-demo"),
                    Map.entry(
                            "spring.datasource.url",
                            "jdbc:h2:mem:timeflows;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false"),
                    Map.entry("spring.datasource.username", "sa"),
                    Map.entry("spring.datasource.password", ""),
                    Map.entry("spring.h2.console.enabled", "true"),
                    Map.entry(
                            "timeflows.jwt.secret",
                            "test-only-jwt-secret-that-is-long-enough-for-hmac-signing"),
                    Map.entry("timeflows.jwt.expiration", "PT2H"),
                    Map.entry("timeflows.mfa.enabled", "false"),
                    Map.entry(
                            "timeflows.mfa.encryption-key",
                            "test-only-mfa-encryption-key-32-bytes"),
                    Map.entry("timeflows.mfa.issuer", "timeFlows-demo"),
                    Map.entry("timeflows.bootstrap.admin-password", "test-only-admin-password"));

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        apply(applicationContext.getEnvironment());
    }

    void apply(ConfigurableEnvironment environment) {
        if (environment.getProperty("timeflows.demo-data.enabled", Boolean.class, true)) {
            environment
                    .getPropertySources()
                    .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, DEMO_PROPERTIES));
        }
    }
}
