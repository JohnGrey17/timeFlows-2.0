package example.timeflows.service;

import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.exception.OvertimeException;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.OvertimeRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OvertimeServiceImpl implements OvertimeService {

    private final OvertimeRepository overtimeRepository;
    private final UserService userService;

    public OvertimeServiceImpl(OvertimeRepository overtimeRepository, UserService userService) {
        this.overtimeRepository = overtimeRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Overtime> findMonth(String userEmail, YearMonth month) {
        return overtimeRepository.findByUserEmailAndWorkDateBetweenOrderByWorkDateAsc(
                userEmail, month.atDay(1), month.atEndOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Overtime> findUserMonth(Long userId, YearMonth month) {
        return overtimeRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, month.atDay(1), month.atEndOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Overtime> findDivisionMonth(Long divisionId, YearMonth month) {
        return overtimeRepository
                .findByUserDivisionIdAndWorkDateBetweenOrderByUserEmailAscWorkDateAsc(
                        divisionId, month.atDay(1), month.atEndOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Overtime> findDepartmentMonth(Long departmentId, YearMonth month) {
        return overtimeRepository
                .findByUserDivisionDepartmentIdAndWorkDateBetweenOrderByUserEmailAscWorkDateAsc(
                        departmentId, month.atDay(1), month.atEndOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public Overtime findByIdForUser(Long id, String userEmail) {
        return overtimeRepository
                .findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new OvertimeException("Overtime з id " + id + " не знайдено"));
    }

    @Override
    @Transactional(readOnly = true)
    public Overtime findById(Long id) {
        return overtimeRepository
                .findWithUserById(id)
                .orElseThrow(() -> new OvertimeException("Overtime з id " + id + " не знайдено"));
    }

    @Override
    @Transactional
    public Overtime create(String userEmail, OvertimeRequest request) {
        if (overtimeRepository.existsByUserEmailAndWorkDate(userEmail, request.getWorkDate())) {
            throw new OvertimeException("На один день можна створити тільки один overtime");
        }
        validateHours(request);

        User user = userService.findByEmail(userEmail);
        Overtime overtime = new Overtime();
        overtime.setUser(user);
        overtime.setWorkDate(request.getWorkDate());
        overtime.setHours(request.getHours());
        overtime.setDescription(request.getDescription());
        if (user.getRoles().contains(example.timeflows.model.Role.ADMIN)) {
            overtime.setStatus(OvertimeStatus.APPROVED);
            overtime.setManagerComment("Автоматично погоджено для адміністратора");
        } else {
            overtime.setStatus(OvertimeStatus.PENDING);
        }
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime update(String userEmail, Long id, OvertimeRequest request) {
        Overtime overtime = findByIdForUser(id, userEmail);
        assertOwnerCanChange(userEmail, overtime);
        validateHours(request);
        overtimeRepository
                .findByUserEmailAndWorkDate(userEmail, request.getWorkDate())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(
                        existing -> {
                            throw new OvertimeException(
                                    "На один день можна створити тільки один overtime");
                        });

        overtime.setWorkDate(request.getWorkDate());
        overtime.setHours(request.getHours());
        overtime.setDescription(request.getDescription());
        overtime.setUpdatedAt(LocalDateTime.now());
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public void delete(String userEmail, Long id) {
        Overtime overtime = findByIdForUser(id, userEmail);
        assertOwnerCanChange(userEmail, overtime);
        overtimeRepository.delete(overtime);
    }

    @Override
    @Transactional
    public Overtime resubmit(String userEmail, Long id, OvertimeRequest request) {
        Overtime overtime = findByIdForUser(id, userEmail);
        if (overtime.getStatus() != OvertimeStatus.REJECTED) {
            throw new OvertimeException(
                    "Повторно на погодження можна відправити тільки відхилений overtime");
        }
        if (request.getResubmissionReason() == null || request.getResubmissionReason().isBlank()) {
            throw new OvertimeException("Причина повторного погодження обов'язкова");
        }
        validateHours(request);
        overtime.setHours(request.getHours());
        overtime.setDescription(request.getDescription());
        overtime.setResubmissionReason(request.getResubmissionReason());
        overtime.setManagerComment(null);
        overtime.setStatus(OvertimeStatus.PENDING);
        overtime.setUpdatedAt(LocalDateTime.now());
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime approve(Long id, String managerComment) {
        Overtime overtime = findById(id);
        assertAwaitingDecision(overtime);
        overtime.setStatus(OvertimeStatus.APPROVED);
        overtime.setManagerComment(managerComment);
        overtime.setUpdatedAt(LocalDateTime.now());
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime approve(Long id, String managerComment, String reviewerEmail) {
        assertCanReview(id, reviewerEmail);
        return approve(id, managerComment);
    }

    @Override
    @Transactional
    public Overtime reject(Long id, String managerComment) {
        if (managerComment == null || managerComment.isBlank()) {
            throw new OvertimeException("Причина відхилення є обов'язковою");
        }
        Overtime overtime = findById(id);
        assertAwaitingDecision(overtime);
        overtime.setStatus(OvertimeStatus.REJECTED);
        overtime.setManagerComment(managerComment);
        overtime.setUpdatedAt(LocalDateTime.now());
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime reject(Long id, String managerComment, String reviewerEmail) {
        assertCanReview(id, reviewerEmail);
        return reject(id, managerComment);
    }

    private void assertCanReview(Long overtimeId, String reviewerEmail) {
        User reviewer = userService.findByEmail(reviewerEmail);
        if (reviewer.getRoles().contains(Role.ADMIN)) {
            return;
        }
        Overtime overtime = findById(overtimeId);
        if (!overtime.getUser().getDivision().getId().equals(reviewer.getDivision().getId())) {
            throw new example.timeflows.exception.UserException(
                    "Керівник може переглядати тільки overtime свого відділу");
        }
    }

    private void validateHours(OvertimeRequest request) {
        double maxHours = isWeekend(request) ? 14.0 : 6.0;
        if (request.getHours() > maxHours) {
            throw new OvertimeException(
                    "Максимальна кількість overtime для цього дня: " + maxHours + " годин");
        }
    }

    private boolean isWeekend(OvertimeRequest request) {
        DayOfWeek day = request.getWorkDate().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private void assertPending(Overtime overtime) {
        if (overtime.getStatus() != OvertimeStatus.PENDING) {
            throw new OvertimeException("Погоджений або відхилений overtime не можна змінювати");
        }
    }

    private void assertOwnerCanChange(String userEmail, Overtime overtime) {
        if (overtime.getStatus() == OvertimeStatus.PENDING) return;
        User user = userService.findByEmail(userEmail);
        boolean privileged =
                user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.MANAGER);
        if (!privileged || !YearMonth.from(overtime.getWorkDate()).equals(YearMonth.now())) {
            throw new OvertimeException(
                    "Змінювати або видаляти завершений overtime може лише адміністратор чи керівник у поточному місяці");
        }
    }

    private void assertAwaitingDecision(Overtime overtime) {
        if (overtime.getStatus() != OvertimeStatus.PENDING) {
            throw new OvertimeException(
                    "Рішення можна прийняти тільки для overtime, що очікує погодження");
        }
    }
}
