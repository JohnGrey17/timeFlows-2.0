package example.timeflows;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.model.Role;
import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.OvertimeRepository;
import example.timeflows.repository.UserRepository;
import example.timeflows.service.DemoDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:demodata;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
            "timeflows.demo-data.enabled=true"
        })
class DemoDataIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private DivisionRepository divisions;
    @Autowired private OvertimeRepository overtimes;
    @Autowired private BonusRepository bonuses;
    @Autowired private DemoDataService demoDataService;

    @Test
    @Transactional
    void createsCompleteIdempotentPresentationDataset() {
        assertThat(users.count()).isEqualTo(6);
        assertThat(overtimes.count()).isEqualTo(4);
        assertThat(bonuses.count()).isEqualTo(4);
        assertThat(users.findByEmail("admin@vyriy.com").orElseThrow().getRoles())
                .contains(Role.ADMIN, Role.EMPLOYEE);
        assertThat(users.findByEmail("it.manager@vyriy.com").orElseThrow().getRoles())
                .contains(Role.MANAGER, Role.EMPLOYEE);
        assertThat(divisions.findById(1L).orElseThrow().getManager().getEmail())
                .isEqualTo("it.manager@vyriy.com");
        assertThat(divisions.findById(2L).orElseThrow().getManager().getEmail())
                .isEqualTo("architect.manager@vyriy.com");

        demoDataService.initialize();

        assertThat(users.count()).isEqualTo(6);
        assertThat(overtimes.count()).isEqualTo(4);
        assertThat(bonuses.count()).isEqualTo(4);
    }
}
