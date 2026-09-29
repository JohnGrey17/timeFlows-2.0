package example.timeflows;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.model.BusinessTag;
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
        assertThat(users.count()).isEqualTo(29);
        assertThat(departments.count()).isEqualTo(3);
        assertThat(directorates.count()).isEqualTo(17);
        assertThat(divisions.count()).isEqualTo(57);
        assertThat(subdivisions.count()).isEqualTo(163);
        assertThat(overtimes.count()).isEqualTo(6);
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
                .hasSize(5);
        assertThat(divisions.findAll().stream().filter(division -> division.getManager() != null))
                .hasSize(9);
        var demonstrationDirector = users.findByEmail("demo.director@vyriy.com").orElseThrow();
        var demonstrationManager = users.findByEmail("demo.manager@vyriy.com").orElseThrow();
        assertThat(demonstrationDirector.getRoles()).contains(Role.DIRECTORATE_MANAGER);
        assertThat(demonstrationDirector.getTags()).contains(BusinessTag.FULL_MANAGEMENT);
        assertThat(demonstrationDirector.getDivision().getDirectorate().getName())
                .isEqualTo("Управління для демонстрації");
        assertThat(demonstrationManager.getRoles()).contains(Role.MANAGER);
        assertThat(demonstrationManager.getTags()).contains(BusinessTag.FULL_MANAGEMENT);
        var demonstrationOfficeManager =
                users.findByEmail("demo.office.manager@vyriy.com").orElseThrow();
        assertThat(demonstrationOfficeManager.getRoles()).contains(Role.OFFICE_MANAGER);
        assertThat(demonstrationOfficeManager.getTags()).contains(BusinessTag.FULL_MANAGEMENT);
        assertThat(demonstrationOfficeManager.getSubdivision().getName())
                .isEqualTo("Демонстраційна команда 2");
        assertThat(demonstrationOfficeManager.getSubdivision().getManager())
                .isEqualTo(demonstrationOfficeManager);
        assertThat(
                        users.findAll().stream()
                                .filter(
                                        user ->
                                                user.getDivision()
                                                        .getName()
                                                        .equals("Демонстраційний відділ")))
                .hasSize(10);
        assertThat(users.findAll())
                .allSatisfy(
                        user -> {
                            assertThat(user.getDivision()).isNotNull();
                        });
        assertThat(
                        users.findAll().stream()
                                .filter(user -> user.getSubdivision() == null)
                                .map(user -> user.getEmail()))
                .containsExactlyInAnyOrder(
                        "demo.no.subdivision1@vyriy.com", "demo.no.subdivision2@vyriy.com");

        demoDataService.initialize();

        assertThat(users.count()).isEqualTo(29);
        assertThat(departments.count()).isEqualTo(3);
        assertThat(directorates.count()).isEqualTo(17);
        assertThat(divisions.count()).isEqualTo(57);
        assertThat(subdivisions.count()).isEqualTo(163);
        assertThat(overtimes.count()).isEqualTo(6);
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
