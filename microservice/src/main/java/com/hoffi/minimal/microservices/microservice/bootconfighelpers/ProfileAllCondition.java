package com.hoffi.minimal.microservices.microservice.bootconfighelpers;

import java.util.LinkedList;
import java.util.List;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

class ProfileAllCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (context.getEnvironment() != null) {
            MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(ProfileAll.class.getName());
            if (attrs != null) {
                LinkedList<String> profilesActive = new LinkedList<>();
                LinkedList<String> profilesNotActive = new LinkedList<>();
                List<Object> values = attrs.get("value");
                int count = 0;
                for (Object value : values) {
                    for (String profile : ((String[]) value)) {
                        count++;
                        if (context.getEnvironment().acceptsProfiles(Profiles.of(profile))) {
                            profilesActive.add(profile);
                        } else {
                            profilesNotActive.add(profile);
                        }
                    }
                }
                if (profilesActive.size() == count) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
