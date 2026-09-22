package example.timeflows.service;

import example.timeflows.exception.UserException;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import org.springframework.stereotype.Service;

@Service
public class ManagementAccessServiceImpl implements ManagementAccessService {

    private final UserService userService;
    private final BonusService bonusService;
    private final AccessPolicy accessPolicy;

    public ManagementAccessServiceImpl(
            UserService userService, BonusService bonusService, AccessPolicy accessPolicy) {
        this.userService = userService;
        this.bonusService = bonusService;
        this.accessPolicy = accessPolicy;
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
        if (!accessPolicy.isAbsolut(actor)
                && !actor.getRoles().contains(Role.ADMIN)
                && !(actor.getRoles().contains(Role.DIRECTORATE_MANAGER)
                        && actor.getDivision() != null
                        && actor.getDivision().getDirectorate() != null
                        && target.getDivision() != null
                        && target.getDivision().getDirectorate() != null
                        && actor.getDivision()
                                .getDirectorate()
                                .getId()
                                .equals(target.getDivision().getDirectorate().getId()))
                && !actor.getDivision().getId().equals(target.getDivision().getId())) {
            throw new UserException("Керівник може працювати лише зі своїм відділом");
        }
    }
}
