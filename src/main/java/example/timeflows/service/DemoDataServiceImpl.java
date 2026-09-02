package example.timeflows.service;

import example.timeflows.model.Bonus;
import example.timeflows.model.BonusCategory;
import example.timeflows.model.BonusStatus;
import example.timeflows.model.BonusType;
import example.timeflows.model.BusinessTag;
import example.timeflows.model.Department;
import example.timeflows.model.Directorate;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.Subdivision;
import example.timeflows.model.User;
import example.timeflows.repository.BonusCategoryRepository;
import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.DepartmentRepository;
import example.timeflows.repository.DirectorateRepository;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.OvertimeRepository;
import example.timeflows.repository.SubdivisionRepository;
import example.timeflows.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataServiceImpl implements DemoDataService {
    private final UserRepository users;
    private final DivisionRepository divisions;
    private final DepartmentRepository departments;
    private final DirectorateRepository directorates;
    private final SubdivisionRepository subdivisions;
    private final OvertimeRepository overtimes;
    private final BonusRepository bonuses;
    private final BonusCategoryRepository categories;
    private final PasswordEncoder passwordEncoder;
    private final String initialAdminPassword;

    public DemoDataServiceImpl(
            UserRepository users,
            DivisionRepository divisions,
            DepartmentRepository departments,
            DirectorateRepository directorates,
            SubdivisionRepository subdivisions,
            OvertimeRepository overtimes,
            BonusRepository bonuses,
            BonusCategoryRepository categories,
            PasswordEncoder passwordEncoder,
            @Value("${timeflows.bootstrap.admin-password:}") String initialAdminPassword) {
        this.users = users;
        this.divisions = divisions;
        this.departments = departments;
        this.directorates = directorates;
        this.subdivisions = subdivisions;
        this.overtimes = overtimes;
        this.bonuses = bonuses;
        this.categories = categories;
        this.passwordEncoder = passwordEncoder;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    @Transactional
    public void initialize() {
        if (initialAdminPassword == null || initialAdminPassword.isBlank()) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD is required when demo data bootstrap is enabled");
        }
        Department department = department("Масштабування");
        Directorate technical = directorate(department, "Технічне управління");
        Division it = division(department, technical, "IT");
        removeUnusedSeedDivision(department, "Архітектори");
        removeUnusedSeedDivision(department, "Аналітики");

        User admin =
                user(
                        "serhii.hainovskyi@vyriy.com",
                        "Serhii",
                        "Hainovskyi",
                        initialAdminPassword,
                        it,
                        null,
                        Set.of(BusinessTag.ABSOLUT),
                        new BigDecimal("5000.00"),
                        Role.ADMIN,
                        Role.EMPLOYEE);
        admin.setPassword(passwordEncoder.encode(initialAdminPassword));
        users.save(admin);
        retireLegacyAdmin(admin.getId());
    }

    private User user(
            String email,
            String firstName,
            String lastName,
            String password,
            Division division,
            Subdivision subdivision,
            Set<BusinessTag> tags,
            BigDecimal salary,
            Role... roles) {
        User user = users.findByEmail(email).orElseGet(User::new);
        boolean created = user.getId() == null;
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        if (created) user.setPassword(passwordEncoder.encode(password));
        user.setDivision(division);
        user.setSubdivision(subdivision);
        user.setTags(new LinkedHashSet<>(tags));
        user.setSalary(salary);
        user.setRoles(new LinkedHashSet<>(Set.of(roles)));
        if (user.getRoles().contains(Role.ADMIN)) {
            user.setActive(true);
            user.setDeactivationReason(null);
        }
        return users.save(user);
    }

    private void manager(Division division, User manager) {
        division.setManager(manager);
        divisions.save(division);
    }

    private void retireLegacyAdmin(Long replacementAdminId) {
        users.findByEmail("admin@vyriy.com")
                .filter(legacy -> !legacy.getId().equals(replacementAdminId))
                .ifPresent(
                        legacy -> {
                            legacy.setRoles(new LinkedHashSet<>(Set.of(Role.EMPLOYEE)));
                            legacy.setActive(false);
                            legacy.setDeactivationReason(
                                    "Замінено demo-адміністратором serhii.hainovskyi@vyriy.com");
                            users.save(legacy);
                        });
    }

    private void removeUnusedSeedDivision(Department department, String name) {
        divisions
                .findByDepartmentIdAndNameIgnoreCase(department.getId(), name)
                .ifPresent(divisions::delete);
    }

    private Department department(String name) {
        return departments
                .findByNameIgnoreCase(name)
                .orElseGet(
                        () -> {
                            Department value = new Department();
                            value.setName(name);
                            value.setDescription("Структура для ручного тестування");
                            return departments.save(value);
                        });
    }

    private Directorate directorate(Department department, String name) {
        return directorates
                .findByDepartmentIdAndNameIgnoreCase(department.getId(), name)
                .orElseGet(
                        () -> {
                            Directorate value = new Directorate();
                            value.setName(name);
                            value.setDescription("Demo-управління");
                            value.setDepartment(department);
                            return directorates.save(value);
                        });
    }

    private Division division(Department department, Directorate directorate, String name) {
        Division value =
                divisions
                        .findByDepartmentIdAndNameIgnoreCase(department.getId(), name)
                        .orElseGet(Division::new);
        value.setName(name);
        value.setDescription("Demo-відділ");
        value.setDepartment(department);
        value.setDirectorate(directorate);
        return divisions.save(value);
    }

    private Subdivision subdivision(Division division, String name) {
        return subdivisions
                .findByDivisionIdAndNameIgnoreCase(division.getId(), name)
                .orElseGet(
                        () -> {
                            Subdivision value = new Subdivision();
                            value.setName(name);
                            value.setDescription("Demo-підвідділ");
                            value.setDivision(division);
                            return subdivisions.save(value);
                        });
    }

    private BonusCategory monthlyCategory() {
        return categories.findByActiveTrueOrderByTypeAscNameAsc().stream()
                .filter(category -> category.getType() == BonusType.MONTHLY)
                .findFirst()
                .orElseGet(
                        () -> {
                            BonusCategory value = new BonusCategory();
                            value.setName("Demo місячний бонус");
                            value.setType(BonusType.MONTHLY);
                            value.setActive(true);
                            return categories.save(value);
                        });
    }

    private void archiveMonthlyBonuses(User projectManager) {
        bonuses
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        projectManager.getId(),
                        YearMonth.now().atDay(1).atStartOfDay(),
                        YearMonth.now().plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .filter(bonus -> bonus.getType() == BonusType.MONTHLY)
                .filter(bonus -> !bonus.isArchived())
                .forEach(
                        bonus -> {
                            bonus.setArchived(true);
                            bonuses.save(bonus);
                        });
    }

    private void overtime(
            User user, int day, double hours, String description, OvertimeStatus status) {
        LocalDate date = YearMonth.now().atDay(Math.min(day, YearMonth.now().lengthOfMonth()));
        if (overtimes.existsByUserEmailAndWorkDate(user.getEmail(), date)) return;
        Overtime overtime = new Overtime();
        overtime.setUser(user);
        overtime.setWorkDate(date);
        overtime.setHours(hours);
        overtime.setDescription(description);
        overtime.setStatus(status);
        if (status == OvertimeStatus.APPROVED_ADMIN || status == OvertimeStatus.APPROVED_MANAGER)
            overtime.setManagerComment("Погоджено для демо");
        if (status == OvertimeStatus.DECLINED) overtime.setManagerComment("Потрібне уточнення");
        overtimes.save(overtime);
    }

    private void bonus(
            User user,
            User creator,
            BonusCategory category,
            String description,
            String amount,
            BonusStatus status) {
        bonus(user, creator, category, BonusType.MONTHLY, description, amount, status);
    }

    private void bonus(
            User user,
            User creator,
            BonusCategory category,
            BonusType type,
            String description,
            String amount,
            BonusStatus status) {
        boolean exists =
                bonuses
                        .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                                user.getId(),
                                YearMonth.now().atDay(1).atStartOfDay(),
                                YearMonth.now().plusMonths(1).atDay(1).atStartOfDay())
                        .stream()
                        .anyMatch(value -> description.equals(value.getDescription()));
        if (exists) return;
        Bonus bonus = new Bonus();
        bonus.setUser(user);
        bonus.setCreatedBy(creator);
        bonus.setCategory(category);
        bonus.setType(type);
        bonus.setDescription(description);
        bonus.setAmount(new BigDecimal(amount));
        bonus.setStatus(status);
        if (status != BonusStatus.PENDING) bonus.setAdminComment("Рішення для демонстрації");
        bonuses.save(bonus);
    }
}
