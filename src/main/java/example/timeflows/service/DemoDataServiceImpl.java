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
        Department scaling = department("Масштабування");
        Directorate technical = directorate(scaling, "Технічне управління");
        Directorate commercial = directorate(scaling, "Комерційне управління");
        Division it = division(scaling, technical, "IT");
        Division data = division(scaling, technical, "Дані та автоматизація");
        Division sales = division(scaling, commercial, "Продажі");
        Division marketing = division(scaling, commercial, "Маркетинг");
        Subdivision platform = subdivision(it, "Платформа");
        Subdivision support = subdivision(it, "Підтримка");
        Subdivision analytics = subdivision(data, "Аналітика");
        Subdivision integrations = subdivision(data, "Інтеграції");
        Subdivision b2b = subdivision(sales, "B2B");
        Subdivision b2g = subdivision(sales, "B2G");
        Subdivision content = subdivision(marketing, "Контент");
        Subdivision performance = subdivision(marketing, "Performance-маркетинг");
        removeUnusedSeedDivision(scaling, "Архітектори");
        removeUnusedSeedDivision(scaling, "Аналітики");

        Department operationsDepartment = department("Операційна діяльність");
        Directorate operations = directorate(operationsDepartment, "Операційне управління");
        Directorate people = directorate(operationsDepartment, "Управління персоналом");
        Division logistics = division(operationsDepartment, operations, "Логістика");
        Division procurement = division(operationsDepartment, operations, "Закупівлі");
        Division hr = division(operationsDepartment, people, "HR");
        Division administration = division(operationsDepartment, people, "Адміністративний відділ");
        Subdivision planning = subdivision(logistics, "Планування");
        Subdivision fleet = subdivision(logistics, "Автопарк");
        Subdivision suppliers = subdivision(procurement, "Постачальники");
        Subdivision contracts = subdivision(procurement, "Договори");
        Subdivision recruiting = subdivision(hr, "Рекрутинг");
        Subdivision learning = subdivision(hr, "Навчання та розвиток");
        Subdivision office = subdivision(administration, "Офіс");
        Subdivision documentation = subdivision(administration, "Документообіг");

        createLargeOrganizationForInterfaceTesting();

        User admin =
                user(
                        "serhii.hainovskyi@vyriy.com",
                        "Serhii",
                        "Hainovskyi",
                        initialAdminPassword,
                        it,
                        platform,
                        Set.of(BusinessTag.ABSOLUT),
                        new BigDecimal("5000.00"),
                        Role.ADMIN,
                        Role.EMPLOYEE);
        admin.setPassword(passwordEncoder.encode(initialAdminPassword));
        users.save(admin);

        User olena =
                demoUser(
                        "olena.koval@vyriy.com",
                        "Олена",
                        "Коваль",
                        it,
                        platform,
                        Role.MANAGER,
                        Role.DIRECTORATE_MANAGER);
        demoUser("ihor.melnyk@vyriy.com", "Ігор", "Мельник", it, support);
        User taras =
                demoUser(
                        "taras.bondar@vyriy.com", "Тарас", "Бондар", data, analytics, Role.MANAGER);
        demoUser("maria.shevchenko@vyriy.com", "Марія", "Шевченко", data, integrations);
        User andrii =
                demoUser(
                        "andrii.tkachenko@vyriy.com",
                        "Андрій",
                        "Ткаченко",
                        sales,
                        b2b,
                        Role.MANAGER,
                        Role.DIRECTORATE_MANAGER);
        demoUser("oleksii.kravets@vyriy.com", "Олексій", "Кравець", sales, b2g);
        User sofia =
                demoUser(
                        "sofia.moroz@vyriy.com",
                        "Софія",
                        "Мороз",
                        marketing,
                        content,
                        Role.MANAGER);
        demoUser("dmytro.oliinyk@vyriy.com", "Дмитро", "Олійник", marketing, performance);
        User nataliia =
                demoUser(
                        "nataliia.kozak@vyriy.com",
                        "Наталія",
                        "Козак",
                        logistics,
                        planning,
                        Role.MANAGER,
                        Role.DIRECTORATE_MANAGER);
        demoUser("roman.levchenko@vyriy.com", "Роман", "Левченко", logistics, fleet);
        User bohdan =
                demoUser(
                        "bohdan.polishchuk@vyriy.com",
                        "Богдан",
                        "Поліщук",
                        procurement,
                        suppliers,
                        Role.MANAGER);
        demoUser("yuliia.mazur@vyriy.com", "Юлія", "Мазур", procurement, contracts);
        User maksym =
                demoUser(
                        "maksym.rudenko@vyriy.com",
                        "Максим",
                        "Руденко",
                        hr,
                        recruiting,
                        Role.MANAGER,
                        Role.DIRECTORATE_MANAGER);
        User kateryna =
                demoUser(
                        "kateryna.lisova@vyriy.com",
                        "Катерина",
                        "Лісова",
                        administration,
                        office,
                        Role.MANAGER);

        manager(it, olena);
        manager(data, taras);
        manager(sales, andrii);
        manager(marketing, sofia);
        manager(logistics, nataliia);
        manager(procurement, bohdan);
        manager(hr, maksym);
        manager(administration, kateryna);
        directorateManager(technical, olena);
        directorateManager(commercial, andrii);
        directorateManager(operations, nataliia);
        directorateManager(people, maksym);
        retireLegacyAdmin(admin.getId());
    }

    private void createLargeOrganizationForInterfaceTesting() {
        Department testDepartment = department("Тестовий великий департамент");
        for (int directorateNumber = 1; directorateNumber <= 12; directorateNumber++) {
            Directorate directorate =
                    directorate(
                            testDepartment,
                            "Тестове управління " + String.format("%02d", directorateNumber));
            for (int divisionNumber = 1; divisionNumber <= 4; divisionNumber++) {
                Division division =
                        division(
                                testDepartment,
                                directorate,
                                String.format(
                                        "Відділ %02d.%02d",
                                        directorateNumber, divisionNumber));
                for (int subdivisionNumber = 1;
                        subdivisionNumber <= 3;
                        subdivisionNumber++) {
                    subdivision(
                            division,
                            String.format(
                                    "Підвідділ %02d.%02d.%02d",
                                    directorateNumber, divisionNumber, subdivisionNumber));
                }
            }
        }
    }

    private User demoUser(
            String email,
            String firstName,
            String lastName,
            Division division,
            Subdivision subdivision,
            Role... additionalRoles) {
        LinkedHashSet<Role> roles = new LinkedHashSet<>();
        roles.add(Role.EMPLOYEE);
        roles.addAll(Set.of(additionalRoles));
        User demoUser =
                user(
                        email,
                        firstName,
                        lastName,
                        initialAdminPassword,
                        division,
                        subdivision,
                        Set.of(),
                        new BigDecimal("1800.00"),
                        roles.toArray(Role[]::new));
        demoUser.setPassword(passwordEncoder.encode(initialAdminPassword));
        return users.save(demoUser);
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

    private void directorateManager(Directorate directorate, User manager) {
        directorate.setManager(manager);
        directorates.save(directorate);
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
