package example.timeflows.service;

import example.timeflows.model.*;
import example.timeflows.repository.BonusCategoryRepository;
import example.timeflows.repository.BonusRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BonusServiceImpl implements BonusService {
    private final BonusRepository repository;
    private final BonusCategoryRepository categoryRepository;
    private final UserService userService;
    private final AccessPolicy accessPolicy;

    public BonusServiceImpl(
            BonusRepository repository,
            BonusCategoryRepository categoryRepository,
            UserService userService,
            AccessPolicy accessPolicy) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public List<Bonus> findMonth(YearMonth month) {
        return repository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .filter(bonus -> !bonus.isArchived())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Bonus> findUserMonth(Long userId, YearMonth month) {
        return repository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        userId,
                        month.atDay(1).atStartOfDay(),
                        month.plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .filter(bonus -> !bonus.isArchived())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Bonus> findDivisionMonth(Long divisionId, YearMonth month) {
        return repository
                .findByUserDivisionIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        divisionId,
                        month.atDay(1).atStartOfDay(),
                        month.plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .filter(bonus -> !bonus.isArchived())
                .toList();
    }

    @Transactional(readOnly = true)
    public Bonus find(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Бонус не знайдено"));
    }

    @Transactional
    public Bonus create(
            Long userId,
            Long categoryId,
            BigDecimal amount,
            String description,
            String creatorEmail) {
        return create(userId, categoryId, BonusType.MONTHLY, amount, description, creatorEmail);
    }

    @Override
    @Transactional
    public Bonus create(
            Long userId,
            Long categoryId,
            BonusType type,
            BigDecimal amount,
            String description,
            String creatorEmail) {
        BonusType requestedType = type == null ? BonusType.MONTHLY : type;
        validateAmount(amount);
        if (requestedType == BonusType.QUARTERLY) {
            throw new IllegalArgumentException(
                    "Квартальний бонус створюється лише через квартальний розподіл KPI");
        }
        BonusCategory selectedCategory = null;
        if (requestedType == BonusType.MONTHLY) {
            selectedCategory = category(categoryId);
            if (!selectedCategory.isActive()) {
                throw new IllegalArgumentException("Категорія бонусу архівована");
            }
        }
        User target = userService.findById(userId);
        User creator = userService.findByEmail(creatorEmail);
        validateTypeAccess(requestedType, creator, target);
        Bonus bonus = new Bonus();
        bonus.setUser(target);
        bonus.setCreatedBy(creator);
        bonus.setCategory(selectedCategory);
        bonus.setType(requestedType);
        bonus.setAmount(amount);
        bonus.setDescription(normalize(description));
        if (requestedType == BonusType.KPI) bonus.setStatus(BonusStatus.APPROVED);
        return repository.save(bonus);
    }

    @Transactional
    public Bonus update(
            Long id, Long categoryId, BigDecimal amount, String description, boolean allowFinal) {
        Bonus b = find(id);
        if (!allowFinal) assertPending(b);
        if (b.getType() == BonusType.QUARTERLY) {
            throw new IllegalArgumentException("Квартальний бонус змінюється лише перерозподілом");
        }
        validateAmount(amount);
        b.setCategory(b.getType() == BonusType.MONTHLY ? category(categoryId) : null);
        b.setAmount(amount);
        b.setDescription(normalize(description));
        b.setUpdatedAt(LocalDateTime.now());
        return repository.save(b);
    }

    @Transactional
    public void delete(Long id, boolean allowFinal) {
        Bonus bonus = find(id);
        if (!allowFinal) assertPending(bonus);
        bonus.setArchived(true);
        bonus.setUpdatedAt(LocalDateTime.now());
        repository.save(bonus);
    }

    @Transactional
    public Bonus decide(Long id, BonusStatus status, String comment) {
        return decide(id, status, comment, false);
    }

    @Override
    @Transactional
    public Bonus decide(Long id, BonusStatus status, String comment, boolean allowFinal) {
        Bonus b = find(id);
        if (!allowFinal) assertPending(b);
        b.setStatus(status);
        b.setAdminComment(comment);
        b.setUpdatedAt(LocalDateTime.now());
        return repository.save(b);
    }

    @Override
    @Transactional
    public List<Bonus> distributeQuarterly(
            int year, int quarter, Long categoryId, Set<Long> userIds, String creatorEmail) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Квартал має бути від 1 до 4");
        }
        Set<Long> selectedIds = userIds == null ? Set.of() : new LinkedHashSet<>(userIds);
        if (selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Оберіть хоча б одного отримувача");
        }
        User creator = userService.findByEmail(creatorEmail);
        if (!creator.getRoles().contains(Role.ADMIN) && !accessPolicy.isAbsolut(creator)) {
            throw new IllegalArgumentException("Квартальний бонус розподіляє лише ADMIN");
        }
        if (!findQuarterlyDistribution(year, quarter).isEmpty()) {
            throw new IllegalArgumentException(
                    "Квартальний бонус за Q" + quarter + " " + year + " вже розподілено");
        }
        BigDecimal pool = quarterlyPool(year, quarter);
        if (pool.signum() <= 0) {
            throw new IllegalArgumentException("У вибраному кварталі немає погоджених KPI");
        }
        List<User> recipients = selectedIds.stream().map(userService::findById).toList();
        for (User recipient : recipients) {
            if (!recipient.isActive()) {
                throw new IllegalArgumentException(
                        "Не можна включити деактивованого користувача: " + recipient.getEmail());
            }
            if (hasEffectiveTag(recipient, BusinessTag.PROJECT_MANAGER)
                    && !recipient.getTags().contains(BusinessTag.PROJECT_MANAGER_LEAD)) {
                throw new IllegalArgumentException("PROJECT_MANAGER не отримує квартальний бонус");
            }
            if (repository.existsByUserIdAndTypeAndQuarterYearAndQuarterNumber(
                    recipient.getId(), BonusType.QUARTERLY, year, quarter)) {
                throw new IllegalArgumentException(
                        "Користувач уже має квартальний бонус за вибраний квартал: "
                                + recipient.getEmail());
            }
        }
        BigDecimal base = pool.divide(BigDecimal.valueOf(recipients.size()), 2, RoundingMode.DOWN);
        int remainderCents =
                pool.subtract(base.multiply(BigDecimal.valueOf(recipients.size())))
                        .movePointRight(2)
                        .intValueExact();
        List<Bonus> distributed = new ArrayList<>();
        for (int index = 0; index < recipients.size(); index++) {
            Bonus bonus = new Bonus();
            bonus.setUser(recipients.get(index));
            bonus.setCreatedBy(creator);
            bonus.setCategory(null);
            bonus.setType(BonusType.QUARTERLY);
            bonus.setQuarterYear(year);
            bonus.setQuarterNumber(quarter);
            bonus.setAmount(index < remainderCents ? base.add(new BigDecimal("0.01")) : base);
            bonus.setDescription("Квартальний бонус Q" + quarter + " " + year);
            bonus.setStatus(BonusStatus.APPROVED);
            distributed.add(bonus);
        }
        return repository.saveAll(distributed);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal quarterlyPool(int year, int quarter) {
        validateQuarter(quarter);
        LocalDateTime from = YearMonth.of(year, (quarter - 1) * 3 + 1).atDay(1).atStartOfDay();
        return repository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(from, from.plusMonths(3))
                .stream()
                .filter(
                        bonus ->
                                bonus.getType() == BonusType.KPI
                                        && bonus.getStatus() == BonusStatus.APPROVED
                                        && !bonus.isArchived())
                .map(Bonus::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bonus> findQuarterlyDistribution(int year, int quarter) {
        validateQuarter(quarter);
        return repository.findByTypeAndQuarterYearAndQuarterNumberOrderByUserEmailAsc(
                BonusType.QUARTERLY, year, quarter);
    }

    @Override
    @Transactional
    public int resetQuarterlyDistribution(int year, int quarter, String actorEmail) {
        User actor = userService.findByEmail(actorEmail);
        if (!actor.getRoles().contains(Role.ADMIN) && !accessPolicy.isAbsolut(actor)) {
            throw new IllegalArgumentException("Скинути розподіл може лише ADMIN");
        }
        List<Bonus> distribution = findQuarterlyDistribution(year, quarter);
        if (distribution.isEmpty()) {
            throw new IllegalArgumentException(
                    "Квартальний бонус за Q" + quarter + " " + year + " ще не розподілено");
        }
        repository.deleteAll(distribution);
        return distribution.size();
    }

    private void validateQuarter(int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Квартал має бути від 1 до 4");
        }
    }

    @Transactional(readOnly = true)
    public List<BonusCategory> findCategories() {
        return categoryRepository.findByActiveTrueOrderByTypeAscNameAsc().stream()
                .filter(category -> category.getType() == BonusType.MONTHLY)
                .toList();
    }

    @Transactional
    public BonusCategory createCategory(String name) {
        return createCategory(name, BonusType.MONTHLY);
    }

    @Override
    @Transactional
    public BonusCategory createCategory(String name, BonusType type) {
        String value = categoryName(name);
        BonusType requestedType = type == null ? BonusType.MONTHLY : type;
        if (requestedType != BonusType.MONTHLY)
            throw new IllegalArgumentException("Категорії доступні лише для місячних бонусів");
        if (categoryRepository.existsByTypeAndNameIgnoreCase(requestedType, value))
            throw new IllegalArgumentException("Категорія з такою назвою вже існує");
        BonusCategory c = new BonusCategory();
        c.setName(value);
        c.setType(requestedType);
        c.setActive(true);
        return categoryRepository.save(c);
    }

    @Transactional
    public BonusCategory updateCategory(Long id, String name) {
        BonusCategory existing = category(id);
        return updateCategory(id, name, existing.getType());
    }

    @Override
    @Transactional
    public BonusCategory updateCategory(Long id, String name, BonusType type) {
        String value = categoryName(name);
        BonusType requestedType = type == null ? BonusType.MONTHLY : type;
        if (requestedType != BonusType.MONTHLY)
            throw new IllegalArgumentException("Категорії доступні лише для місячних бонусів");
        if (categoryRepository.existsByTypeAndNameIgnoreCaseAndIdNot(requestedType, value, id))
            throw new IllegalArgumentException("Категорія з такою назвою вже існує");
        BonusCategory c = category(id);
        c.setName(value);
        c.setType(requestedType);
        return categoryRepository.save(c);
    }

    @Transactional
    public void deleteCategory(Long id) {
        BonusCategory category = category(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Сума бонусу має бути більшою за 0");
    }

    private BonusCategory category(Long id) {
        if (id == null) throw new IllegalArgumentException("Оберіть категорію бонусу");
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категорію бонусу не знайдено"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String categoryName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Назва категорії обов'язкова");
        return name.trim();
    }

    private void assertPending(Bonus b) {
        if (b.getStatus() != BonusStatus.PENDING)
            throw new IllegalArgumentException("Змінювати можна лише бонус, що очікує рішення");
    }

    private void validateTypeAccess(BonusType type, User creator, User target) {
        if (accessPolicy.isAbsolut(creator)) return;
        boolean targetProjectManager =
                hasEffectiveTag(target, BusinessTag.PROJECT_MANAGER)
                        && !target.getTags().contains(BusinessTag.PROJECT_MANAGER_LEAD);
        if (type == BonusType.KPI) {
            if (!creator.getTags().contains(BusinessTag.PROJECT_MANAGER_LEAD)) {
                throw new IllegalArgumentException("KPI може створювати лише PROJECT_MANAGER_LEAD");
            }
            if (!creator.getDivision().getId().equals(target.getDivision().getId())) {
                throw new IllegalArgumentException("PM Lead працює лише зі своїм відділом");
            }
            if (!targetProjectManager
                    || target.getTags().contains(BusinessTag.PROJECT_MANAGER_LEAD)) {
                throw new IllegalArgumentException("KPI доступний лише PROJECT_MANAGER");
            }
        } else if (type == BonusType.MONTHLY && targetProjectManager) {
            throw new IllegalArgumentException(
                    "PROJECT_MANAGER не отримує погоджений місячний бонус");
        } else if (type == BonusType.QUARTERLY && !creator.getRoles().contains(Role.ADMIN)) {
            throw new IllegalArgumentException("Квартальний бонус розподіляє лише ADMIN");
        }
    }

    private boolean hasEffectiveTag(User user, BusinessTag tag) {
        return user.getTags().contains(tag)
                || (user.getDivision() != null && user.getDivision().getTags().contains(tag));
    }
}
