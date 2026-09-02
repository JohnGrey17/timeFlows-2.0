package example.timeflows.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DemoEnvironmentInitializerTests {

    private final DemoEnvironmentInitializer initializer = new DemoEnvironmentInitializer();

    @Test
    void replacesExternalConfigurationWhenDemoDataIsEnabled() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty("timeflows.demo-data.enabled", "true")
                        .withProperty("spring.datasource.url", "jdbc:postgresql://production/db")
                        .withProperty("timeflows.jwt.secret", "production-secret");

        initializer.apply(environment);

        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:timeflows");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("sa");
        assertThat(environment.getProperty("spring.h2.console.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("timeflows.jwt.secret"))
                .startsWith("test-only-jwt-secret");
        assertThat(environment.getProperty("timeflows.mfa.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("timeflows.access.absolut-enabled")).isEqualTo("true");
        assertThat(environment.getProperty("timeflows.bootstrap.admin-password"))
                .isEqualTo("test-only-admin-password");
    }

    @Test
    void preservesExternalConfigurationWhenDemoDataIsDisabled() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty("timeflows.demo-data.enabled", "false")
                        .withProperty("spring.datasource.url", "jdbc:postgresql://production/db")
                        .withProperty("timeflows.jwt.secret", "production-secret");

        initializer.apply(environment);

        assertThat(
                        environment
                                .getPropertySources()
                                .contains(DemoEnvironmentInitializer.PROPERTY_SOURCE_NAME))
                .isFalse();
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://production/db");
        assertThat(environment.getProperty("timeflows.jwt.secret")).isEqualTo("production-secret");
    }
}
