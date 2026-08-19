package example.timeflows;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.DivisionRepository;
import example.timeflows.repository.OvertimeRepository;
import example.timeflows.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "timeflows.demo-data.enabled=false")
class TimeflowsApplicationTests {

    @Autowired private UserRepository userRepository;

    @Autowired private DivisionRepository divisionRepository;

    @Autowired private OvertimeRepository overtimeRepository;

    @Autowired private BonusRepository bonusRepository;

    @Test
    void contextLoads() {}

    @Test
    void startsWithoutUsersOvertimesBonusesOrManagers() {
        assertThat(userRepository.count()).isZero();
        assertThat(overtimeRepository.count()).isZero();
        assertThat(bonusRepository.count()).isZero();
        assertThat(divisionRepository.findAll())
                .allSatisfy(division -> assertThat(division.getManager()).isNull());
    }
}
