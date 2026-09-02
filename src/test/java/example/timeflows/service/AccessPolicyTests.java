package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;

import example.timeflows.model.BusinessTag;
import example.timeflows.model.User;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccessPolicyTests {

    @Test
    void recognizesAbsolutOnlyWhenFeatureIsEnabled() {
        User user = new User();
        user.setTags(new LinkedHashSet<>(Set.of(BusinessTag.ABSOLUT)));

        assertThat(new AccessPolicy(true).isAbsolut(user)).isTrue();
        assertThat(new AccessPolicy(false).isAbsolut(user)).isFalse();
    }

    @Test
    void doesNotElevateOrdinaryUsers() {
        assertThat(new AccessPolicy(true).isAbsolut(new User())).isFalse();
    }
}
