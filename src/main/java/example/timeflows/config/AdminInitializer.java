package example.timeflows.config;

import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.model.Division;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.UserRepository;
import example.timeflows.repository.OvertimeRepository;
import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.BonusCategoryRepository;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Bonus;
import example.timeflows.model.BonusStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@vyriy.com";

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final PasswordEncoder passwordEncoder;
    private final OvertimeRepository overtimeRepository;
    private final BonusRepository bonusRepository;
    private final BonusCategoryRepository bonusCategoryRepository;

    public AdminInitializer(
            UserRepository userRepository,
            DivisionRepository divisionRepository,
            PasswordEncoder passwordEncoder, OvertimeRepository overtimeRepository, BonusRepository bonusRepository, BonusCategoryRepository bonusCategoryRepository
    ) {
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.passwordEncoder = passwordEncoder;
        this.overtimeRepository = overtimeRepository;
        this.bonusRepository = bonusRepository;
        this.bonusCategoryRepository = bonusCategoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Division it = divisionRepository.findById(1L).orElseThrow();
        Division architects = divisionRepository.findById(2L).orElseThrow();

        User admin = createUser(ADMIN_EMAIL, "TimeFlows", "Admin", "admin123", it,
                Set.of(Role.ADMIN, Role.EMPLOYEE), new BigDecimal("5000.00"));
        User itManager = createUser("it.manager@vyriy.com", "Іван", "Керівник", "manager123", it,
                Set.of(Role.MANAGER, Role.EMPLOYEE), new BigDecimal("4200.00"));
        User architectManager = createUser("architect.manager@vyriy.com", "Олена", "Керівник", "manager123", architects,
                Set.of(Role.MANAGER, Role.EMPLOYEE), new BigDecimal("4300.00"));
        User andrii = createUser("andrii.employee@vyriy.com", "Андрій", "Коваль", "employee123", it,
                Set.of(Role.EMPLOYEE), new BigDecimal("2500.00"));
        User maria = createUser("maria.employee@vyriy.com", "Марія", "Бондар", "employee123", it,
                Set.of(Role.EMPLOYEE), new BigDecimal("2600.00"));
        User petro = createUser("petro.employee@vyriy.com", "Петро", "Мельник", "employee123", architects,
                Set.of(Role.EMPLOYEE), new BigDecimal("2700.00"));

        assignManager(it, itManager);
        assignManager(architects, architectManager);
        createDemoOvertime(andrii, 3, 2.5, "Підготовка термінового релізу");
        createDemoOvertime(maria, 5, 4.0, "Оновлення внутрішньої документації");
        createDemoOvertime(petro, 7, 3.0, "Термінові архітектурні правки");
        createDemoBonus(andrii, itManager, new BigDecimal("350.00"), "За успішний реліз");
        createDemoBonus(maria, itManager, new BigDecimal("250.00"), "За допомогу команді");
        createDemoBonus(petro, architectManager, new BigDecimal("400.00"), "За архітектурну ініціативу");
    }

    private User createUser(
            String email,
            String firstName,
            String lastName,
            String password,
            Division division,
            Set<Role> roles,
            BigDecimal salary
    ) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(passwordEncoder.encode(password));
            user.setDivision(division);
            user.setRoles(new LinkedHashSet<>(roles));
            user.setSalary(salary);
            return userRepository.save(user);
        });
    }

    private void assignManager(Division division, User manager) {
        if (division.getManager() == null || !division.getManager().getId().equals(manager.getId())) {
            division.setManager(manager);
            divisionRepository.save(division);
        }
    }

    private void createDemoOvertime(User user, int preferredDay, double hours, String description) {
        YearMonth month = YearMonth.now();
        LocalDate date = month.atDay(Math.min(preferredDay, month.lengthOfMonth()));
        if (!overtimeRepository.existsByUserEmailAndWorkDate(user.getEmail(), date)) {
            Overtime overtime = new Overtime(); overtime.setUser(user); overtime.setWorkDate(date);
            overtime.setHours(hours); overtime.setDescription(description); overtime.setStatus(OvertimeStatus.PENDING);
            overtimeRepository.save(overtime);
        }
    }

    private void createDemoBonus(User user, User creator, BigDecimal amount, String description) {
        boolean exists = bonusRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                user.getId(), YearMonth.now().atDay(1).atStartOfDay(), YearMonth.now().plusMonths(1).atDay(1).atStartOfDay())
                .stream().anyMatch(b -> b.getDescription().equals(description));
        if (!exists) {
            Bonus bonus = new Bonus(); bonus.setUser(user); bonus.setCreatedBy(creator); bonus.setAmount(amount);
            bonus.setDescription(description); bonus.setCategory(bonusCategoryRepository.findAllByOrderByNameAsc().get(0)); bonus.setStatus(BonusStatus.PENDING); bonusRepository.save(bonus);
        }
    }
}
