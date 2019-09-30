package testhelpers;

import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public abstract class DTOhelpers {
    // ========================================================================
    // ====   Test Helpers   ==================================================
    // ========================================================================
    public static FeatureMatcher<MessageDTO, String> id(Matcher<String> matcher) {
        return new FeatureMatcher<MessageDTO, String>(matcher, "has an id of", " is id") {
            @Override
            protected String featureValueOf(MessageDTO actual) {
                return actual.seq;
            }
        };
    }

    public static FeatureMatcher<MessageDTO, String> message(Matcher<String> matcher) {
        return new FeatureMatcher<MessageDTO, String>(matcher, "has a message of", "is message") {
            @Override
            protected String featureValueOf(MessageDTO actual) {
                return actual.message;
            }
        };
    }

    public static FeatureMatcher<MessageDTO, String> modifiers(Matcher<String> matcher) {
        return new FeatureMatcher<MessageDTO, String>(matcher, "has modifiers of", "are modifiers") {
            @Override
            protected String featureValueOf(MessageDTO actual) {
                return actual.modifications;
            }
        };
    }

}
