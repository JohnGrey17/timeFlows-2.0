package example.timeflows.service;

import example.timeflows.model.*;
import example.timeflows.repository.BonusRepository;
import example.timeflows.repository.BonusCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BonusServiceTests {
    @Mock BonusRepository repository; @Mock BonusCategoryRepository categoryRepository; @Mock UserService userService; BonusService service;
    @BeforeEach void setup(){service=new BonusServiceImpl(repository,categoryRepository,userService);}
    @Test void approvePendingBonus(){Bonus b=bonus(BonusStatus.PENDING);when(repository.findById(1L)).thenReturn(Optional.of(b));when(repository.save(b)).thenReturn(b);service.decide(1L,BonusStatus.APPROVED,"ok");assertThat(b.getStatus()).isEqualTo(BonusStatus.APPROVED);}
    @Test void cannotEditRejectedBonus(){Bonus b=bonus(BonusStatus.REJECTED);when(repository.findById(1L)).thenReturn(Optional.of(b));assertThatThrownBy(()->service.update(1L,1L,BigDecimal.TEN,"x",false)).isInstanceOf(IllegalArgumentException.class);}
    @Test void managerCannotDeleteFinalBonus(){Bonus b=bonus(BonusStatus.APPROVED);when(repository.findById(1L)).thenReturn(Optional.of(b));assertThatThrownBy(()->service.delete(1L,false)).isInstanceOf(IllegalArgumentException.class);verify(repository,never()).delete(any());}
    @Test void adminCanDeleteFinalBonus(){Bonus b=bonus(BonusStatus.APPROVED);when(repository.findById(1L)).thenReturn(Optional.of(b));service.delete(1L,true);verify(repository).delete(b);}
    private Bonus bonus(BonusStatus status){Bonus b=new Bonus();b.setId(1L);b.setStatus(status);return b;}
}
