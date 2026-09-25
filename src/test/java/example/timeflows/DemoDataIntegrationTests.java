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
import example.timeflows.service.UserService;
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
    @Autowired private UserService userService;

    @Test
    @Transactional
    void createsRichIdempotentPreProductionDataset() {
        assertThat(users.count()).isEqualTo(15);
        assertThat(departments.count()).isEqualTo(3);
        assertThat(directorates.count()).isEqualTo(16);
        assertThat(divisions.count()).isEqualTo(56);
        assertThat(subdivisions.count()).isEqualTo(160);
        assertThat(overtimes.count()).isZero();
        assertThat(bonuses.count()).isZero();

        var admin = users.findByEmail("serhii.hainovskyi@vyriy.com").orElseThrow();
        assertThat(admin.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.EMPLOYEE);
        assertThat(admin.getDivision().getDepartment().getName()).isEqualTo("Масштабування");
        assertThat(admin.getDivision().getDirectorate().getName()).isEqualTo("Технічне управління");
        assertThat(admin.getDivision().getName()).isEqualTo("IT");
        assertThat(admin.getSubdivision().getName()).isEqualTo("Платформа");
        assertThat(departments.findByNameIgnoreCase("Операційна діяльність")).isPresent();
        assertThat(
                        directorates.findAllByOrderByNameAsc().stream()
                                .filter(directorate -> directorate.getManager() != null))
                .hasSize(4);
        assertThat(divisions.findAll().stream().filter(division -> division.getManager() != null))
                .hasSize(8);
        assertThat(users.findAll())
                .allSatisfy(
                        user -> {
                            assertThat(user.getDivision()).isNotNull();
                            assertThat(user.getSubdivision()).isNotNull();
                        });

        demoDataService.initialize();

        assertThat(users.count()).isEqualTo(15);
        assertThat(departments.count()).isEqualTo(3);
        assertThat(directorates.count()).isEqualTo(16);
        assertThat(divisions.count()).isEqualTo(56);
        assertThat(subdivisions.count()).isEqualTo(160);
        assertThat(overtimes.count()).isZero();
        assertThat(bonuses.count()).isZero();
    }

    @Test
    @Transactional
    void permanentlyDeletesDeactivatedUserWithoutForeignKeyFailures() {
        demoDataService.initialize();
        var user = users.findByEmail("ihor.melnyk@vyriy.com").orElseThrow();
        user.setActive(false);
        users.saveAndFlush(user);

        userService.deleteDeactivatedPermanently(user.getId());

        assertThat(users.findById(user.getId())).isEmpty();
    }
}
