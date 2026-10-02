package example.timeflows;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.model.BusinessTag;
import example.timeflows.model.OvertimeStatus;
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
        assertThat(users.count()).isEqualTo(54);
        assertThat(departments.count()).isEqualTo(3);
        assertThat(directorates.count()).isEqualTo(18);
        assertThat(divisions.count()).isEqualTo(59);
        assertThat(subdivisions.count()).isEqualTo(165);
        assertThat(overtimes.count()).isEqualTo(8);
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
                .hasSize(6);
        assertThat(divisions.findAll().stream().filter(division -> division.getManager() != null))
                .hasSize(11);
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
                .hasSize(30);
        assertThat(
                        users.findAll().stream()
                                .filter(user -> user.getEmail().startsWith("demo.scroll.employee")))
                .hasSize(20)
                .allSatisfy(
                        user -> {
                            assertThat(user.getDivision())
                                    .isEqualTo(demonstrationManager.getDivision());
                            assertThat(user.getSubdivision().getName())
                                    .isEqualTo("Демонстраційна команда");
                            assertThat(user.getRoles()).containsExactly(Role.EMPLOYEE);
                        });
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
        var pmDirector = users.findByEmail("pm.test.director@vyriy.com").orElseThrow();
        var pmDivisionManager = users.findByEmail("pm.test.manager@vyriy.com").orElseThrow();
        var pmCoordinator = users.findByEmail("pm.test.coordinator@vyriy.com").orElseThrow();
        var pmDeliveryManager = users.findByEmail("pm.test.delivery@vyriy.com").orElseThrow();
        assertThat(pmDirector.getRoles()).contains(Role.MANAGER, Role.DIRECTORATE_MANAGER);
        assertThat(pmDirector.getTags())
                .contains(BusinessTag.PROJECT_MANAGER_LEAD, BusinessTag.FULL_MANAGEMENT);
        assertThat(pmDirector.getDivision().getDirectorate().getManager()).isEqualTo(pmDirector);
        assertThat(pmDirector.getDivision().getManager()).isEqualTo(pmDirector);
        assertThat(pmDivisionManager.getRoles()).contains(Role.MANAGER);
        assertThat(pmDivisionManager.getDivision().getManager()).isEqualTo(pmDivisionManager);
        assertThat(pmCoordinator.getTags()).contains(BusinessTag.PROJECT_MANAGER);
        assertThat(pmDeliveryManager.getTags()).contains(BusinessTag.PROJECT_MANAGER);
        assertThat(pmCoordinator.getDivision()).isNotEqualTo(pmDeliveryManager.getDivision());
        assertThat(
                        overtimes.findAll().stream()
                                .filter(
                                        overtime ->
                                                overtime.getUser()
                                                        .getEmail()
                                                        .startsWith("pm.test.")))
                .hasSize(2)
                .allSatisfy(
                        overtime -> {
                            assertThat(overtime.getStatus())
                                    .isEqualTo(OvertimeStatus.APPROVED_DIRECTORATE);
                            assertThat(overtime.getManagerComment())
                                    .isEqualTo("Погоджено для демо");
                        });

        demoDataService.initialize();

        assertThat(users.count()).isEqualTo(54);
        assertThat(departments.count()).isEqualTo(3);
        assertThat(directorates.count()).isEqualTo(18);
        assertThat(divisions.count()).isEqualTo(59);
        assertThat(subdivisions.count()).isEqualTo(165);
        assertThat(overtimes.count()).isEqualTo(8);
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
