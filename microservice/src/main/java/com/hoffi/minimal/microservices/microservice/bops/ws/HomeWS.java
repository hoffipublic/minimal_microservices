package com.hoffi.minimal.microservices.microservice.bops.ws;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hoffi.minimal.microservices.microservice.helpers.RESThelper;

@RestController
@RequestMapping("/")
public class HomeWS {

    @Value("${my.test.property}")
    String myTestProperty;

    @GetMapping(produces = "application/json")
    public Map<RequestMethod, Set<String>> home() {
        return RESThelper.requestMappings("", new String[] { "", "WS" }, HomeWS.class.getPackage());
    }

    @GetMapping(value = "/config", produces = "application/json")
    public String config() {
        return myTestProperty;
    }
}
