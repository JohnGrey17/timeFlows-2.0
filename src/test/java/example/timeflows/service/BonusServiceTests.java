package example.timeflows.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import example.timeflows.model.*;
import example.timeflows.repository.BonusCategoryRepository;
import example.timeflows.repository.BonusRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BonusServiceTests {
    @Mock BonusRepository repository;
    @Mock BonusCategoryRepository categoryRepository;
    @Mock UserService userService;
    BonusService service;

    @BeforeEach
    void setup() {
        service = new BonusServiceImpl(repository, categoryRepository, userService);
    }

    @Test
    void approvePendingBonus() {
        Bonus b = bonus(BonusStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        when(repository.save(b)).thenReturn(b);
        service.decide(1L, BonusStatus.APPROVED, "ok");
        assertThat(b.getStatus()).isEqualTo(BonusStatus.APPROVED);
    }

    @Test
    void cannotEditRejectedBonus() {
        Bonus b = bonus(BonusStatus.REJECTED);
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        assertThatThrownBy(() -> service.update(1L, 1L, BigDecimal.TEN, "x", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void managerCannotDeleteFinalBonus() {
        Bonus b = bonus(BonusStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        assertThatThrownBy(() -> service.delete(1L, false))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void adminCanDeleteFinalBonus() {
        Bonus b = bonus(BonusStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        service.delete(1L, true);
        verify(repository).delete(b);
    }

    @Test
    void monthQueriesUseExactMonthBoundaries() {
        YearMonth month = YearMonth.of(2026, 8);
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 0, 0);
        Bonus bonus = bonus(BonusStatus.PENDING);
        when(repository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end))
                .thenReturn(List.of(bonus));
        when(repository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(1L, start, end))
                .thenReturn(List.of(bonus));
        when(repository.findByUserDivisionIdAndCreatedAtBetweenOrderByCreatedAtDesc(2L, start, end))
                .thenReturn(List.of(bonus));

        assertThat(service.findMonth(month)).containsExactly(bonus);
        assertThat(service.findUserMonth(1L, month)).containsExactly(bonus);
        assertThat(service.findDivisionMonth(2L, month)).containsExactly(bonus);
    }

    @Test
    void createBuildsAndNormalizesBonus() {
        User employee = new User();
        User creator = new User();
        BonusCategory category = category(3L, "Performance");
        when(userService.findById(1L)).thenReturn(employee);
        when(userService.findByEmail("manager@vyriy.com")).thenReturn(creator);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(repository.save(any(Bonus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bonus result =
                service.create(1L, 3L, BigDecimal.TEN, "  Excellent work  ", "manager@vyriy.com");

        assertThat(result.getUser()).isSameAs(employee);
        assertThat(result.getCreatedBy()).isSameAs(creator);
        assertThat(result.getCategory()).isSameAs(category);
        assertThat(result.getDescription()).isEqualTo("Excellent work");
    }

    @Test
    void createRejectsInvalidAmountAndCategory() {
        assertThatThrownBy(() -> service.create(1L, 1L, BigDecimal.ZERO, "", "a@vyriy.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(1L, null, BigDecimal.ONE, "", "a@vyriy.com"))
                .isInstanceOf(IllegalArgumentException.class);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(1L, 99L, BigDecimal.ONE, "", "a@vyriy.com"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userService);
    }

    @Test
    void updatePendingBonusChangesEditableFields() {
        Bonus bonus = bonus(BonusStatus.PENDING);
        BonusCategory category = category(2L, "Teamwork");
        when(repository.findById(1L)).thenReturn(Optional.of(bonus));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(repository.save(bonus)).thenReturn(bonus);

        Bonus result = service.update(1L, 2L, BigDecimal.TEN, null, false);

        assertThat(result.getCategory()).isSameAs(category);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(result.getDescription()).isEmpty();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void categoryCrudValidatesNamesAndReportsDeleteFailure() {
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category(1L, "A")));
        assertThat(service.findCategories())
                .extracting(BonusCategory::getName)
                .containsExactly("A");

        when(categoryRepository.save(any(BonusCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BonusCategory created = service.createCategory("  Performance  ");
        assertThat(created.getName()).isEqualTo("Performance");

        when(categoryRepository.existsByNameIgnoreCase("Duplicate")).thenReturn(true);
        assertThatThrownBy(() -> service.createCategory("Duplicate"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createCategory(" "))
                .isInstanceOf(IllegalArgumentException.class);

        BonusCategory existing = category(1L, "Old");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        BonusCategory updated = service.updateCategory(1L, " New ");
        assertThat(updated.getName()).isEqualTo("New");

        doThrow(new RuntimeException("constraint")).when(categoryRepository).flush();
        assertThatThrownBy(() -> service.deleteCategory(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Bonus bonus(BonusStatus status) {
        Bonus b = new Bonus();
        b.setId(1L);
        b.setStatus(status);
        return b;
    }

    private BonusCategory category(Long id, String name) {
        BonusCategory category = new BonusCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
