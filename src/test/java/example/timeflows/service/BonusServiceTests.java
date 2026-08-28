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
import java.util.Set;
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
    void adminArchivesBonusWithoutDeletingHistory() {
        Bonus b = bonus(BonusStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        service.delete(1L, true);
        assertThat(b.isArchived()).isTrue();
        verify(repository).save(b);
        verify(repository, never()).delete(b);
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
    void projectManagerLeadCreatesImmediatelyApprovedKpi() {
        Division division = new Division();
        division.setId(5L);
        User lead = new User();
        lead.setDivision(division);
        lead.setTags(new java.util.LinkedHashSet<>(Set.of(BusinessTag.PROJECT_MANAGER_LEAD)));
        User target = new User();
        target.setDivision(division);
        target.setTags(new java.util.LinkedHashSet<>(Set.of(BusinessTag.PROJECT_MANAGER)));
        when(userService.findById(1L)).thenReturn(target);
        when(userService.findByEmail("lead@vyriy.com")).thenReturn(lead);
        when(repository.save(any(Bonus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bonus result =
                service.create(
                        1L, 3L, BonusType.KPI, BigDecimal.TEN, "August KPI", "lead@vyriy.com");

        assertThat(result.getType()).isEqualTo(BonusType.KPI);
        assertThat(result.getStatus()).isEqualTo(BonusStatus.APPROVED);
        assertThat(result.getCategory()).isNull();
    }

    @Test
    void projectManagerLeadCanCreateKpiForSelf() {
        Division division = new Division();
        division.setId(5L);
        User lead = new User();
        lead.setId(7L);
        lead.setDivision(division);
        lead.setTags(new java.util.LinkedHashSet<>(Set.of(BusinessTag.PROJECT_MANAGER_LEAD)));
        when(userService.findById(7L)).thenReturn(lead);
        when(userService.findByEmail("lead@vyriy.com")).thenReturn(lead);
        when(repository.save(any(Bonus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bonus result =
                service.create(
                        7L,
                        null,
                        BonusType.KPI,
                        new BigDecimal("125.00"),
                        "Lead KPI",
                        "lead@vyriy.com");

        assertThat(result.getUser()).isSameAs(lead);
        assertThat(result.getType()).isEqualTo(BonusType.KPI);
        assertThat(result.getStatus()).isEqualTo(BonusStatus.APPROVED);
    }

    @Test
    void quarterlyDistributionPreservesPoolToTheCent() {
        User admin = new User();
        admin.setRoles(new java.util.LinkedHashSet<>(Set.of(Role.ADMIN)));
        Bonus approvedKpi = bonus(BonusStatus.APPROVED);
        approvedKpi.setType(BonusType.KPI);
        approvedKpi.setAmount(BigDecimal.TEN);
        User first = activeUser(1L, "first@vyriy.com");
        User second = activeUser(2L, "second@vyriy.com");
        User third = activeUser(3L, "third@vyriy.com");
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);
        when(userService.findById(1L)).thenReturn(first);
        when(userService.findById(2L)).thenReturn(second);
        when(userService.findById(3L)).thenReturn(third);
        when(repository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0)))
                .thenReturn(List.of(approvedKpi));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Bonus> result =
                service.distributeQuarterly(
                        2026,
                        3,
                        7L,
                        new java.util.LinkedHashSet<>(List.of(1L, 2L, 3L)),
                        "admin@vyriy.com");

        assertThat(result)
                .extracting(Bonus::getAmount)
                .containsExactly(
                        new BigDecimal("3.34"), new BigDecimal("3.33"), new BigDecimal("3.33"));
        assertThat(result.stream().map(Bonus::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("10.00");
        assertThat(result).allMatch(bonus -> bonus.getStatus() == BonusStatus.APPROVED);
        assertThat(result).allMatch(bonus -> bonus.getCategory() == null);
    }

    @Test
    void quarterlyDistributionReportsMissingPool() {
        User admin = new User();
        admin.setRoles(new java.util.LinkedHashSet<>(Set.of(Role.ADMIN)));
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);

        assertThatThrownBy(
                        () ->
                                service.distributeQuarterly(
                                        2026, 1, null, Set.of(1L), "admin@vyriy.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("немає погоджених KPI");
    }

    @Test
    void quarterlyDistributionReportsAlreadyDistributedQuarter() {
        User admin = new User();
        admin.setRoles(new java.util.LinkedHashSet<>(Set.of(Role.ADMIN)));
        Bonus existing = bonus(BonusStatus.APPROVED);
        existing.setType(BonusType.QUARTERLY);
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);
        when(repository.findByTypeAndQuarterYearAndQuarterNumberOrderByUserEmailAsc(
                        BonusType.QUARTERLY, 2026, 1))
                .thenReturn(List.of(existing));

        assertThatThrownBy(
                        () ->
                                service.distributeQuarterly(
                                        2026, 1, null, Set.of(1L), "admin@vyriy.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("вже розподілено");
    }

    @Test
    void adminCanResetQuarterlyDistribution() {
        User admin = new User();
        admin.setRoles(new java.util.LinkedHashSet<>(Set.of(Role.ADMIN)));
        Bonus first = bonus(BonusStatus.APPROVED);
        Bonus second = bonus(BonusStatus.APPROVED);
        when(userService.findByEmail("admin@vyriy.com")).thenReturn(admin);
        when(repository.findByTypeAndQuarterYearAndQuarterNumberOrderByUserEmailAsc(
                        BonusType.QUARTERLY, 2026, 1))
                .thenReturn(List.of(first, second));

        assertThat(service.resetQuarterlyDistribution(2026, 1, "admin@vyriy.com")).isEqualTo(2);
        verify(repository).deleteAll(List.of(first, second));
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
        when(categoryRepository.findByActiveTrueOrderByTypeAscNameAsc())
                .thenReturn(List.of(category(1L, "A")));
        assertThat(service.findCategories())
                .extracting(BonusCategory::getName)
                .containsExactly("A");

        when(categoryRepository.save(any(BonusCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BonusCategory created = service.createCategory("  Performance  ");
        assertThat(created.getName()).isEqualTo("Performance");

        when(categoryRepository.existsByTypeAndNameIgnoreCase(BonusType.MONTHLY, "Duplicate"))
                .thenReturn(true);
        assertThatThrownBy(() -> service.createCategory("Duplicate"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createCategory(" "))
                .isInstanceOf(IllegalArgumentException.class);

        BonusCategory existing = category(1L, "Old");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        BonusCategory updated = service.updateCategory(1L, " New ");
        assertThat(updated.getName()).isEqualTo("New");

        clearInvocations(categoryRepository);
        service.deleteCategory(1L);
        assertThat(existing.isActive()).isFalse();
        verify(categoryRepository).save(existing);
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

    private User activeUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setActive(true);
        return user;
    }
}
