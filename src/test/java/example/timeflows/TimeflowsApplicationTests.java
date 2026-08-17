package example.timeflows;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TimeflowsApplicationTests {

    @Autowired private UserRepository userRepository;

    @Autowired private DivisionRepository divisionRepository;

    @Test
    void contextLoads() {}

    @Test
    void createsDemoUsersAndDivisionManagers() {
        assertThat(userRepository.findByEmail("admin@vyriy.com")).isPresent();
        assertThat(userRepository.findByEmail("it.manager@vyriy.com")).isPresent();
        assertThat(userRepository.findByEmail("architect.manager@vyriy.com")).isPresent();
        assertThat(userRepository.findByEmail("andrii.employee@vyriy.com")).isPresent();
        assertThat(userRepository.findByEmail("maria.employee@vyriy.com")).isPresent();
        assertThat(userRepository.findByEmail("petro.employee@vyriy.com")).isPresent();
        assertThat(divisionRepository.findById(1L).orElseThrow().getManager()).isNotNull();
        assertThat(divisionRepository.findById(2L).orElseThrow().getManager()).isNotNull();
    }
}
