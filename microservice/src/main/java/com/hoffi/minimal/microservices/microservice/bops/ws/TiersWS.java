package com.hoffi.minimal.microservices.microservice.bops.ws;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.hoffi.minimal.microservices.microservice.businesslogic.BusinessLogic;
import com.hoffi.minimal.microservices.microservice.helpers.RESThelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Profile({ "tier1", "tier2", "tier3" })
@RestController
@RequestMapping("/tiers")
public class TiersWS {

    @Autowired
    private BusinessLogic businessLogic;

    @GetMapping(produces = "application/json")
    public Map<RequestMethod, Set<String>> home() {
        return RESThelper.requestMappings("", this.getClass());
    }

    @GetMapping(value = { "/failevery/{count}", "/failevery" }, produces = "text/plain")
    public String failevery(@PathVariable("count") Optional<Integer> count) {

        Integer theCount = count.isPresent() ? count.get() : 5;
        businessLogic.failOnEveryXthCall(theCount);

        return "X " + " will fail on every " + theCount + "th call";
    }
}
