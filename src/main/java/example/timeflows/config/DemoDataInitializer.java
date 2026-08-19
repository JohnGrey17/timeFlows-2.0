package example.timeflows.config;

import example.timeflows.service.DemoDataService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "timeflows.demo-data.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {
    private final DemoDataService demoDataService;

    public DemoDataInitializer(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @Override
    public void run(String... args) {
        demoDataService.initialize();
    }
}
