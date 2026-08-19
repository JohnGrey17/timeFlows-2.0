package example.timeflows.service;

import example.timeflows.model.Bonus;
import example.timeflows.model.BonusCategory;
import example.timeflows.model.BonusStatus;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.BonusCategoryRepository;
import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.OvertimeRepository;
import example.timeflows.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataServiceImpl implements DemoDataService {
    private final UserRepository users;
    private final DivisionRepository divisions;
    private final OvertimeRepository overtimes;
    private final BonusRepository bonuses;
    private final BonusCategoryRepository categories;
    private final PasswordEncoder passwordEncoder;

    public DemoDataServiceImpl(
            UserRepository users,
            DivisionRepository divisions,
            OvertimeRepository overtimes,
            BonusRepository bonuses,
            BonusCategoryRepository categories,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.divisions = divisions;
        this.overtimes = overtimes;
        this.bonuses = bonuses;
        this.categories = categories;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void initialize() {
        Division it = divisions.findById(1L).orElseThrow();
        Division architects = divisions.findById(2L).orElseThrow();

        User admin =
                user(
                        "admin@vyriy.com",
                        "TimeFlows",
                        "Admin",
                        "TimeFlows-Demo-Admin-2026!",
                        it,
                        new BigDecimal("5000.00"),
                        Role.ADMIN,
                        Role.EMPLOYEE);
        User itManager =
                user(
                        "it.manager@vyriy.com",
                        "Іван",
                        "Керівник",
                        "TimeFlows-Demo-Manager-2026!",
                        it,
                        new BigDecimal("4200.00"),
                        Role.MANAGER,
                        Role.EMPLOYEE);
        User architectManager =
                user(
                        "architect.manager@vyriy.com",
                        "Олена",
                        "Керівник",
                        "TimeFlows-Demo-Manager-2026!",
                        architects,
                        new BigDecimal("4300.00"),
                        Role.MANAGER,
                        Role.EMPLOYEE);
        User andrii =
                user(
                        "andrii.employee@vyriy.com",
                        "Андрій",
                        "Коваль",
                        "TimeFlows-Demo-Employee-2026!",
                        it,
                        new BigDecimal("2500.00"),
                        Role.EMPLOYEE);
        User maria =
                user(
                        "maria.employee@vyriy.com",
                        "Марія",
                        "Бондар",
                        "TimeFlows-Demo-Employee-2026!",
                        it,
                        new BigDecimal("2600.00"),
                        Role.EMPLOYEE);
        User petro =
                user(
                        "petro.employee@vyriy.com",
                        "Петро",
                        "Мельник",
                        "TimeFlows-Demo-Employee-2026!",
                        architects,
                        new BigDecimal("2700.00"),
                        Role.EMPLOYEE);

        manager(it, itManager);
        manager(architects, architectManager);

        overtime(andrii, 3, 2.5, "Підготовка термінового релізу", OvertimeStatus.APPROVED);
        overtime(maria, 5, 4.0, "Оновлення внутрішньої документації", OvertimeStatus.PENDING);
        overtime(petro, 7, 3.0, "Термінові архітектурні правки", OvertimeStatus.REJECTED);
        overtime(itManager, 9, 2.0, "Підтримка production deployment", OvertimeStatus.APPROVED);

        BonusCategory category = categories.findAllByOrderByNameAsc().get(0);
        bonus(andrii, itManager, category, "За успішний реліз", "350.00", BonusStatus.APPROVED);
        bonus(maria, itManager, category, "За допомогу команді", "250.00", BonusStatus.PENDING);
        bonus(
                petro,
                architectManager,
                category,
                "За архітектурну ініціативу",
                "400.00",
                BonusStatus.REJECTED);
        bonus(
                admin,
                admin,
                category,
                "Демонстраційний погоджений бонус",
                "500.00",
                BonusStatus.APPROVED);
    }

    private User user(
            String email,
            String firstName,
            String lastName,
            String password,
            Division division,
            BigDecimal salary,
            Role... roles) {
        return users.findByEmail(email)
                .orElseGet(
                        () -> {
                            User user = new User();
                            user.setUsername(email);
                            user.setEmail(email);
                            user.setFirstName(firstName);
                            user.setLastName(lastName);
                            user.setPassword(passwordEncoder.encode(password));
                            user.setDivision(division);
                            user.setSalary(salary);
                            user.setRoles(new LinkedHashSet<>(Set.of(roles)));
                            return users.save(user);
                        });
    }

    private void manager(Division division, User manager) {
        division.setManager(manager);
        divisions.save(division);
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
        if (status == OvertimeStatus.APPROVED) overtime.setManagerComment("Погоджено для демо");
        if (status == OvertimeStatus.REJECTED) overtime.setManagerComment("Потрібне уточнення");
        overtimes.save(overtime);
    }

    private void bonus(
            User user,
            User creator,
            BonusCategory category,
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
        bonus.setDescription(description);
        bonus.setAmount(new BigDecimal(amount));
        bonus.setStatus(status);
        if (status != BonusStatus.PENDING) bonus.setAdminComment("Рішення для демонстрації");
        bonuses.save(bonus);
    }
}
