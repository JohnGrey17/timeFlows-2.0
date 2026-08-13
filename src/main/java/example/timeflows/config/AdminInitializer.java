package example.timeflows.config;

import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.model.Division;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.math.BigDecimal;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@vyriy.com";

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(
            UserRepository userRepository,
            DivisionRepository divisionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Division it = divisionRepository.findById(1L).orElseThrow();
        Division architects = divisionRepository.findById(2L).orElseThrow();

        createUser(ADMIN_EMAIL, "TimeFlows", "Admin", "admin123", it,
                Set.of(Role.ADMIN, Role.EMPLOYEE), new BigDecimal("5000.00"));
        User itManager = createUser("it.manager@vyriy.com", "Іван", "Керівник", "manager123", it,
                Set.of(Role.MANAGER, Role.EMPLOYEE), new BigDecimal("4200.00"));
        User architectManager = createUser("architect.manager@vyriy.com", "Олена", "Керівник", "manager123", architects,
                Set.of(Role.MANAGER, Role.EMPLOYEE), new BigDecimal("4300.00"));
        createUser("andrii.employee@vyriy.com", "Андрій", "Коваль", "employee123", it,
                Set.of(Role.EMPLOYEE), new BigDecimal("2500.00"));
        createUser("maria.employee@vyriy.com", "Марія", "Бондар", "employee123", it,
                Set.of(Role.EMPLOYEE), new BigDecimal("2600.00"));
        createUser("petro.employee@vyriy.com", "Петро", "Мельник", "employee123", architects,
                Set.of(Role.EMPLOYEE), new BigDecimal("2700.00"));

        assignManager(it, itManager);
        assignManager(architects, architectManager);
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
}
