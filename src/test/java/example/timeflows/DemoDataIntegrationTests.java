package example.timeflows;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.model.Role;
import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.DepartmentRepository;
import example.timeflows.repository.DirectorateRepository;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.OvertimeRepository;
import example.timeflows.repository.SubdivisionRepository;
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
    @Autowired private DepartmentRepository departments;
    @Autowired private DirectorateRepository directorates;
    @Autowired private SubdivisionRepository subdivisions;
    @Autowired private OvertimeRepository overtimes;
    @Autowired private BonusRepository bonuses;
    @Autowired private DemoDataService demoDataService;

    @Test
    @Transactional
    void createsMinimalIdempotentPreProductionDataset() {
        assertThat(users.count()).isEqualTo(1);
        assertThat(departments.count()).isEqualTo(1);
        assertThat(directorates.count()).isEqualTo(1);
        assertThat(divisions.count()).isEqualTo(1);
        assertThat(subdivisions.count()).isZero();
        assertThat(overtimes.count()).isZero();
        assertThat(bonuses.count()).isZero();

        var admin = users.findByEmail("serhii.hainovskyi@vyriy.com").orElseThrow();
        assertThat(admin.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.EMPLOYEE);
        assertThat(admin.getDivision().getDepartment().getName()).isEqualTo("Масштабування");
        assertThat(admin.getDivision().getDirectorate().getName()).isEqualTo("Технічне управління");
        assertThat(admin.getDivision().getName()).isEqualTo("IT");
        assertThat(admin.getSubdivision()).isNull();

        demoDataService.initialize();

        assertThat(users.count()).isEqualTo(1);
        assertThat(departments.count()).isEqualTo(1);
        assertThat(directorates.count()).isEqualTo(1);
        assertThat(divisions.count()).isEqualTo(1);
        assertThat(subdivisions.count()).isZero();
        assertThat(overtimes.count()).isZero();
        assertThat(bonuses.count()).isZero();
    }
}
