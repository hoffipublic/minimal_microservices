package com.hoffi.minimal.microservices.microservice.bops.ws;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.hoffi.minimal.microservices.microservice.bops.outbound.SchedulingRate;
import com.hoffi.minimal.microservices.microservice.bops.outbound.Source;
import com.hoffi.minimal.microservices.microservice.helpers.RESThelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Profile({ "source" })
@RestController
@RequestMapping("/sources")
public class SourcesWS {

    @Autowired
    private SchedulingRate schedulingRate;

    @Value("${app.sources.fixedDelay}")
    private Integer defaultFixedDelay;


    @Autowired
    private Source source;

    @GetMapping(produces = "application/json")
    public Map<RequestMethod, Set<String>> home() {
        return RESThelper.requestMappings("", this.getClass());
    }

    @GetMapping(value = { "/sourcerate/{rate}", "/sourcerate" }, produces = "text/plain")
    public String sourcerate(@PathVariable("rate") Optional<Integer> rate) {

        Integer theRate = rate.isPresent() ? rate.get() : defaultFixedDelay;
        schedulingRate.setFixedDelay(theRate);

        return "X " + " source rate now ms: " + theRate;
    }

    @GetMapping(value = { "/fire" }, produces = "text/plain")
    public String fire() throws Exception {
        source.timerMessageSource();
        return "fired at " + new Date();
    }
}
