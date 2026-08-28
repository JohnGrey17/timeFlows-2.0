package example.timeflows.service;

import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.exception.OvertimeException;
import example.timeflows.model.BusinessTag;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.repository.OvertimeRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OvertimeServiceImpl implements OvertimeService {

    private final OvertimeRepository overtimeRepository;
    private final UserService userService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public OvertimeServiceImpl(OvertimeRepository overtimeRepository, UserService userService) {
        this(overtimeRepository, userService, Clock.system(ZoneId.of("Europe/Kyiv")));
    }

    public OvertimeServiceImpl(
            OvertimeRepository overtimeRepository, UserService userService, Clock clock) {
        this.overtimeRepository = overtimeRepository;
        this.userService = userService;
        this.clock = clock;
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
        User user = userService.findByEmail(userEmail);
        validateSubmission(request.getWorkDate(), user);
        if (overtimeRepository.existsByUserEmailAndWorkDate(userEmail, request.getWorkDate())) {
            throw new OvertimeException("На один день можна створити тільки один overtime");
        }
        validateHours(request, user);

        Overtime overtime = new Overtime();
        overtime.setUser(user);
        overtime.setWorkDate(request.getWorkDate());
        overtime.setHours(request.getHours());
        overtime.setDescription(request.getDescription());
        if (user.getRoles().contains(Role.ADMIN)) {
            overtime.setStatus(OvertimeStatus.APPROVED_ADMIN);
            overtime.setManagerComment("Автоматично погоджено для адміністратора");
        } else if (user.getRoles().contains(Role.MANAGER)) {
            overtime.setStatus(OvertimeStatus.APPROVED_MANAGER);
            overtime.setManagerComment("Автоматично погоджено для керівника відділу");
        } else {
            overtime.setStatus(OvertimeStatus.CHECKING);
        }
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime update(String userEmail, Long id, OvertimeRequest request) {
        Overtime overtime = findByIdForUser(id, userEmail);
        assertOwnerCanChange(userEmail, overtime);
        validateHours(request, overtime.getUser());
        validateSubmission(request.getWorkDate(), overtime.getUser());
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
        if (!isDeclined(overtime.getStatus())) {
            throw new OvertimeException(
                    "Повторно на погодження можна відправити тільки відхилений overtime");
        }
        if (request.getResubmissionReason() == null || request.getResubmissionReason().isBlank()) {
            throw new OvertimeException("Причина повторного погодження обов'язкова");
        }
        validateHours(request, overtime.getUser());
        validateSubmission(overtime.getWorkDate(), overtime.getUser());
        overtime.setHours(request.getHours());
        overtime.setDescription(request.getDescription());
        overtime.setResubmissionReason(request.getResubmissionReason());
        overtime.setManagerComment(null);
        overtime.setStatus(initialStatus(overtime.getUser()));
        overtime.setUpdatedAt(LocalDateTime.now());
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime approve(Long id, String managerComment) {
        Overtime overtime = findById(id);
        if (!isChecking(overtime.getStatus())) {
            throw new OvertimeException(
                    "Рішення Manager можна прийняти тільки для заявки CHECKING");
        }
        overtime.setStatus(OvertimeStatus.APPROVED_MANAGER);
        overtime.setManagerComment(managerComment);
        overtime.setUpdatedAt(LocalDateTime.now());
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime approve(Long id, String managerComment, String reviewerEmail) {
        User reviewer = assertCanReview(id, reviewerEmail);
        Overtime overtime = findById(id);
        validateReviewDeadline(overtime, reviewer);
        if (reviewer.getRoles().contains(Role.ADMIN)) {
            if (overtime.getStatus() != OvertimeStatus.APPROVED_MANAGER) {
                throw new OvertimeException("ADMIN фінально погоджує лише заявку APPROVED_MANAGER");
            }
            overtime.setStatus(OvertimeStatus.APPROVED_ADMIN);
            overtime.setManagerComment(managerComment);
            overtime.setUpdatedAt(LocalDateTime.now(clock));
            return overtimeRepository.save(overtime);
        }
        return approve(id, managerComment);
    }

    @Override
    @Transactional
    public int approveAll(
            java.util.Collection<Long> ids, String managerComment, String reviewerEmail) {
        User reviewer = userService.findByEmail(reviewerEmail);
        if (!reviewer.getRoles().contains(Role.ADMIN)) {
            throw new OvertimeException("Масове погодження доступне лише адміністратору");
        }
        int approved = 0;
        for (Long id : ids.stream().distinct().toList()) {
            Overtime overtime = findById(id);
            if (!canAdminApprove(overtime)) continue;
            assertCanReview(id, reviewerEmail);
            overtime.setStatus(OvertimeStatus.APPROVED_ADMIN);
            overtime.setManagerComment(managerComment);
            overtime.setUpdatedAt(LocalDateTime.now(clock));
            overtimeRepository.save(overtime);
            approved++;
        }
        return approved;
    }

    @Override
    public boolean canAdminApprove(Overtime overtime) {
        if (overtime.getStatus() != OvertimeStatus.APPROVED_MANAGER) return false;
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate overtimeFriday = fridayOfWeek(overtime.getWorkDate());
        LocalDate currentFriday = fridayOfWeek(now.toLocalDate());
        return overtimeFriday.equals(currentFriday)
                && !now.isAfter(overtimeFriday.atTime(LocalTime.of(21, 0)).atZone(now.getZone()));
    }

    @Override
    @Transactional
    public Overtime reject(Long id, String managerComment) {
        if (managerComment == null || managerComment.isBlank()) {
            throw new OvertimeException("Причина відхилення є обов'язковою");
        }
        Overtime overtime = findById(id);
        if (!isAwaitingReview(overtime.getStatus())) {
            throw new OvertimeException("Відхилити можна лише заявку на погодженні");
        }
        overtime.setStatus(OvertimeStatus.DECLINED);
        overtime.setManagerComment(managerComment);
        overtime.setUpdatedAt(LocalDateTime.now());
        return overtimeRepository.save(overtime);
    }

    @Override
    @Transactional
    public Overtime reject(Long id, String managerComment, String reviewerEmail) {
        User reviewer = assertCanReview(id, reviewerEmail);
        validateReviewDeadline(findById(id), reviewer);
        return reject(id, managerComment);
    }

    private User assertCanReview(Long overtimeId, String reviewerEmail) {
        User reviewer = userService.findByEmail(reviewerEmail);
        if (reviewer.getRoles().contains(Role.ADMIN)) {
            return reviewer;
        }
        Overtime overtime = findById(overtimeId);
        if (!overtime.getUser().getDivision().getId().equals(reviewer.getDivision().getId())) {
            throw new example.timeflows.exception.UserException(
                    "Керівник може переглядати тільки overtime свого відділу");
        }
        if (reviewer.getDivision().getManager() == null
                || !reviewer.getDivision().getManager().getId().equals(reviewer.getId())) {
            throw new example.timeflows.exception.UserException(
                    "Погоджувати заявки може лише керівник відділу");
        }
        return reviewer;
    }

    private void validateHours(OvertimeRequest request, User user) {
        if (!isWeekend(request) && !allowsCurrentWeekOvertime(user)) {
            throw new OvertimeException("Перепрацювання дозволені лише у вихідні дні");
        }
        double maxHours = 14.0;
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
        if (!isChecking(overtime.getStatus())) {
            throw new OvertimeException("Погоджений або відхилений overtime не можна змінювати");
        }
    }

    private void assertOwnerCanChange(String userEmail, Overtime overtime) {
        if (isChecking(overtime.getStatus())) return;
        User user = userService.findByEmail(userEmail);
        boolean privileged =
                user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.MANAGER);
        if (!privileged || !YearMonth.from(overtime.getWorkDate()).equals(YearMonth.now())) {
            throw new OvertimeException(
                    "Змінювати або видаляти завершений overtime може лише адміністратор чи керівник у поточному місяці");
        }
    }

    private void validateSubmission(LocalDate workDate, User user) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        if (allowsCurrentWeekOvertime(user)) {
            LocalDate weekStart =
                    now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            if (workDate.isBefore(weekStart) || workDate.isAfter(weekEnd)) {
                throw new OvertimeException(
                        "З тегом ALLOW_OVER заявку можна створити лише в межах поточного тижня");
            }
            return;
        }
        LocalDate friday = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        LocalDate saturday = friday.plusDays(1);
        LocalDate sunday = friday.plusDays(2);
        if (!workDate.equals(saturday) && !workDate.equals(sunday)) {
            throw new OvertimeException("Заявку можна подати лише на поточні вихідні");
        }
        if (now.isAfter(friday.atTime(LocalTime.of(11, 0)).atZone(now.getZone()))) {
            throw new OvertimeException("Дедлайн подання заявки — п'ятниця 11:00 Europe/Kyiv");
        }
    }

    private boolean allowsCurrentWeekOvertime(User user) {
        return user.getTags().contains(BusinessTag.ALLOW_OVER);
    }

    private void validateReviewDeadline(Overtime overtime, User reviewer) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate friday = fridayOfWeek(overtime.getWorkDate());
        LocalTime deadline =
                reviewer.getRoles().contains(Role.ADMIN)
                        ? LocalTime.of(21, 0)
                        : LocalTime.of(14, 0);
        if (now.isAfter(friday.atTime(deadline).atZone(now.getZone()))) {
            throw new OvertimeException(
                    "Дедлайн погодження минув: п'ятниця " + deadline + " Europe/Kyiv");
        }
    }

    private LocalDate fridayOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(4);
    }

    private OvertimeStatus initialStatus(User user) {
        if (user.getRoles().contains(Role.ADMIN)) return OvertimeStatus.APPROVED_ADMIN;
        if (user.getRoles().contains(Role.MANAGER)) return OvertimeStatus.APPROVED_MANAGER;
        return OvertimeStatus.CHECKING;
    }

    private boolean isChecking(OvertimeStatus status) {
        return status == OvertimeStatus.CHECKING || status == OvertimeStatus.PENDING;
    }

    private boolean isDeclined(OvertimeStatus status) {
        return status == OvertimeStatus.DECLINED || status == OvertimeStatus.REJECTED;
    }

    private boolean isAwaitingReview(OvertimeStatus status) {
        return isChecking(status) || status == OvertimeStatus.APPROVED_MANAGER;
    }
}
