package example.timeflows.service;

import example.timeflows.exception.UserException;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import org.springframework.stereotype.Service;

@Service
public class ManagementAccessServiceImpl implements ManagementAccessService {

    private final UserService userService;
    private final BonusService bonusService;

    public ManagementAccessServiceImpl(UserService userService, BonusService bonusService) {
        this.userService = userService;
        this.bonusService = bonusService;
    }

    @Override
    public void assertCanManageUser(String actorEmail, Long targetUserId) {
        assertCanManage(userService.findByEmail(actorEmail), userService.findById(targetUserId));
    }

    @Override
    public void assertCanEditBonus(String actorEmail, Long bonusId) {
        assertCanManage(userService.findByEmail(actorEmail), bonusService.find(bonusId).getUser());
    }

    @Override
    public void assertCanManage(User actor, User target) {
        if (!actor.getRoles().contains(Role.ADMIN)
                && !actor.getDivision().getId().equals(target.getDivision().getId())) {
            throw new UserException("Керівник може працювати лише зі своїм відділом");
        }
    }
}
