package example.timeflows.service;

import example.timeflows.model.BusinessTag;
import example.timeflows.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Central switch for the temporary unrestricted ABSOLUT business tag. */
@Component
public class AccessPolicy {
    private final boolean absolutEnabled;

    public AccessPolicy(@Value("${timeflows.access.absolut-enabled:true}") boolean absolutEnabled) {
        this.absolutEnabled = absolutEnabled;
    }

    public boolean isAbsolut(User user) {
        return absolutEnabled
                && user != null
                && user.getTags() != null
                && user.getTags().contains(BusinessTag.ABSOLUT);
    }
}
