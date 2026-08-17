package example.timeflows.service;

import example.timeflows.model.User;

public interface ManagementAccessService {
    void assertCanManageUser(String actorEmail, Long targetUserId);

    void assertCanEditBonus(String actorEmail, Long bonusId);

    void assertCanManage(User actor, User target);
}
