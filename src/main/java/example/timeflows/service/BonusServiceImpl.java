package example.timeflows.service;

import example.timeflows.model.*;
import example.timeflows.repository.BonusCategoryRepository;
import example.timeflows.repository.BonusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
public class BonusServiceImpl implements BonusService {
    private final BonusRepository repository;
    private final BonusCategoryRepository categoryRepository;
    private final UserService userService;

    public BonusServiceImpl(BonusRepository repository, BonusCategoryRepository categoryRepository, UserService userService) {
        this.repository = repository; this.categoryRepository = categoryRepository; this.userService = userService;
    }

    @Transactional(readOnly = true) public List<Bonus> findMonth(YearMonth month) { return repository.findByCreatedAtBetweenOrderByCreatedAtDesc(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()); }
    @Transactional(readOnly = true) public List<Bonus> findUserMonth(Long userId, YearMonth month) { return repository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()); }
    @Transactional(readOnly = true) public List<Bonus> findDivisionMonth(Long divisionId, YearMonth month) { return repository.findByUserDivisionIdAndCreatedAtBetweenOrderByCreatedAtDesc(divisionId, month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()); }
    @Transactional(readOnly = true) public Bonus find(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Бонус не знайдено")); }

    @Transactional public Bonus create(Long userId, Long categoryId, BigDecimal amount, String description, String creatorEmail) {
        validate(amount, categoryId); Bonus bonus = new Bonus(); bonus.setUser(userService.findById(userId)); bonus.setCreatedBy(userService.findByEmail(creatorEmail)); bonus.setCategory(category(categoryId)); bonus.setAmount(amount); bonus.setDescription(normalize(description)); return repository.save(bonus);
    }
    @Transactional public Bonus update(Long id, Long categoryId, BigDecimal amount, String description, boolean allowFinal) { Bonus b=find(id); if(!allowFinal)assertPending(b); validate(amount, categoryId); b.setCategory(category(categoryId)); b.setAmount(amount); b.setDescription(normalize(description)); b.setUpdatedAt(LocalDateTime.now()); return repository.save(b); }
    @Transactional public void delete(Long id, boolean allowFinal) { Bonus b=find(id); if (!allowFinal) assertPending(b); repository.delete(b); }
    @Transactional public Bonus decide(Long id, BonusStatus status, String comment) { Bonus b=find(id); assertPending(b); b.setStatus(status); b.setAdminComment(comment); b.setUpdatedAt(LocalDateTime.now()); return repository.save(b); }
    @Transactional(readOnly = true) public List<BonusCategory> findCategories(){ return categoryRepository.findAllByOrderByNameAsc(); }
    @Transactional public BonusCategory createCategory(String name){String value=categoryName(name); if(categoryRepository.existsByNameIgnoreCase(value))throw new IllegalArgumentException("Категорія з такою назвою вже існує"); BonusCategory c=new BonusCategory(); c.setName(value); return categoryRepository.save(c);}
    @Transactional public BonusCategory updateCategory(Long id,String name){String value=categoryName(name); if(categoryRepository.existsByNameIgnoreCaseAndIdNot(value,id))throw new IllegalArgumentException("Категорія з такою назвою вже існує"); BonusCategory c=category(id); c.setName(value); return categoryRepository.save(c);}
    @Transactional public void deleteCategory(Long id){try{categoryRepository.delete(category(id));categoryRepository.flush();}catch(Exception e){throw new IllegalArgumentException("Не можна видалити категорію, яка вже використовується в бонусах");}}
    private void validate(BigDecimal amount,Long categoryId){if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("Сума бонусу має бути більшою за 0");category(categoryId);}
    private BonusCategory category(Long id){if(id==null)throw new IllegalArgumentException("Оберіть категорію бонусу");return categoryRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Категорію бонусу не знайдено"));}
    private String normalize(String value){return value==null?"":value.trim();}
    private String categoryName(String name){if(name==null||name.isBlank())throw new IllegalArgumentException("Назва категорії обов'язкова");return name.trim();}
    private void assertPending(Bonus b){if(b.getStatus()!=BonusStatus.PENDING)throw new IllegalArgumentException("Змінювати можна лише бонус, що очікує рішення");}
}
