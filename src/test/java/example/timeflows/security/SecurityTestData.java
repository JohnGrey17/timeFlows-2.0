package example.timeflows.security;

import example.timeflows.model.Bonus;
import example.timeflows.model.BonusStatus;
import example.timeflows.model.Division;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.BonusCategoryRepository;
import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
class SecurityTestData {

    @Bean
    CommandLineRunner securityTestUsers(
            UserRepository users,
            DivisionRepository divisions,
            BonusRepository bonuses,
            BonusCategoryRepository categories,
            PasswordEncoder passwordEncoder) {
        return args -> {
            Division it = divisions.findById(1L).orElseThrow();
            Division architects = divisions.findById(2L).orElseThrow();

            create(users, passwordEncoder, "admin@vyriy.com", it, Role.ADMIN, Role.EMPLOYEE);
            User itManager =
                    create(
                            users,
                            passwordEncoder,
                            "it.manager@vyriy.com",
                            it,
                            Role.MANAGER,
                            Role.EMPLOYEE);
            User architectManager =
                    create(
                            users,
                            passwordEncoder,
                            "architect.manager@vyriy.com",
                            architects,
                            Role.MANAGER,
                            Role.EMPLOYEE);
            User andrii =
                    create(users, passwordEncoder, "andrii.employee@vyriy.com", it, Role.EMPLOYEE);
            User maria =
                    create(users, passwordEncoder, "maria.employee@vyriy.com", it, Role.EMPLOYEE);
            User petro =
                    create(
                            users,
                            passwordEncoder,
                            "petro.employee@vyriy.com",
                            architects,
                            Role.EMPLOYEE);

            it.setManager(itManager);
            architects.setManager(architectManager);
            divisions.saveAll(Set.of(it, architects));

            createBonus(bonuses, categories, andrii, itManager);
            createBonus(bonuses, categories, maria, itManager);
            createBonus(bonuses, categories, petro, architectManager);
        };
    }

    private User create(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            String email,
            Division division,
            Role... roles) {
        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword(passwordEncoder.encode("test-password"));
        user.setDivision(division);
        user.setRoles(new LinkedHashSet<>(Set.of(roles)));
        user.setSalary(new BigDecimal("1000.00"));
        return users.save(user);
    }

    private void createBonus(
            BonusRepository bonuses,
            BonusCategoryRepository categories,
            User user,
            User createdBy) {
        Bonus bonus = new Bonus();
        bonus.setUser(user);
        bonus.setCreatedBy(createdBy);
        bonus.setCategory(categories.findAllByOrderByNameAsc().get(0));
        bonus.setAmount(new BigDecimal("100.00"));
        bonus.setDescription("Test bonus");
        bonus.setStatus(BonusStatus.PENDING);
        bonuses.save(bonus);
    }
}
