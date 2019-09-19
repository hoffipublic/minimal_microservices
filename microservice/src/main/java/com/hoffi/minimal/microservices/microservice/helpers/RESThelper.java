package com.hoffi.minimal.microservices.microservice.helpers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class RESThelper {
    private static final Logger LOG = LoggerFactory.getLogger(RESThelper.class);

    private RESThelper() {
    }

    // RequestMapping,GetMapping,PostMapping,PutMapping,DeleteMapping
    public static Map<RequestMethod, Set<String>> requestMappings(String prefix, Class<?>... clazzes) throws IllegalStateException {
        Map<RequestMethod, Set<String>> classesEndpoints = new TreeMap<>();
        for (Class<?> clazz : clazzes) {
            RequestMapping clazzAnnotationRequestMapping = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
            if (clazzAnnotationRequestMapping == null) {
                throw new IllegalStateException("class " + clazz.getSimpleName() + " has no @RequestMapping at class level!");
            }
            String[] clazzRequestMappingPaths = clazzAnnotationRequestMapping.path();
            String clazzRequestMappingValue = (clazzRequestMappingPaths.length == 0 ? "" : clazzRequestMappingPaths[0]);

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                Set<RequestMapping> methodAnnotationRequestMappings = AnnotatedElementUtils.findAllMergedAnnotations(method, RequestMapping.class);
                for (RequestMapping methodAnnotationRequestMapping : methodAnnotationRequestMappings) {
                    if (methodAnnotationRequestMapping == null) {
                        continue;
                    }
                    for (RequestMethod httpMethod : methodAnnotationRequestMapping.method()) {
                        String[] paths = methodAnnotationRequestMapping.path();
                        for (String value : paths) {
                            add(classesEndpoints, httpMethod, prefix + (clazzRequestMappingValue.equals("/") ? "" : clazzRequestMappingValue) + value);
                        }
                        if (paths.length == 0) {
                            add(classesEndpoints, httpMethod, prefix + clazzRequestMappingValue);
                        }
                    }
                }
            }
            if (classesEndpoints.size() == 0) {
                LOG.info("class " + clazz.getSimpleName() + " has no @RequestMapping Methods!");
            }
        }
        return classesEndpoints;
    }

    public static Map<RequestMethod, Set<String>> requestMappings(String prefix, String[] classnamePrefixPostfix, Package... packages) throws IllegalStateException {

        // create scanner and disable default filters (that is the 'false' argument)
        final ClassPathScanningCandidateComponentProvider classpathScanner = new ClassPathScanningCandidateComponentProvider(false);
        // add include filters which matches all the classes (or use your own)
        String classnamePrefix = "";
        String classnamePostfix = "";
        try {
            classnamePrefix = classnamePrefixPostfix[0];
            classnamePostfix = classnamePrefixPostfix[1];
        } catch (Exception e) {
            // ignore and take defaults
        }
        classpathScanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(classnamePrefix + ".+" + classnamePostfix)));

        Set<BeanDefinition> clazzes = new HashSet<>();
        for (Package packag : packages) {
            // get matching classes defined in the package
            Set<BeanDefinition> hereClasses = classpathScanner.findCandidateComponents(packag.getName());
            clazzes.addAll(hereClasses);
        }

        Map<RequestMethod, Set<String>> classesEndpoints = new TreeMap<>();
        ArrayList<Class<?>> theClasses = new ArrayList<>();
        for (BeanDefinition bd : clazzes) {
            Class<?> clazz;
            try {
                clazz = Class.forName(bd.getBeanClassName());
                theClasses.add(clazz);
            } catch (ClassNotFoundException e) {
                LOG.info(e.getMessage());
                continue;
            }

            Class<?>[] x = new Class<?>[] {};
            Map<RequestMethod, Set<String>> requestMappings = requestMappings("", theClasses.toArray(x));
            for (Entry<RequestMethod, Set<String>> entry : requestMappings.entrySet()) {
                for (String s : entry.getValue()) {
                    add(classesEndpoints, entry.getKey(), s);
                }
            }

        }
        return classesEndpoints;
    }

    private static void add(Map<RequestMethod, Set<String>> map, RequestMethod httpMethod, String path) {
        Set<String> paths = map.get(httpMethod);
        if (paths == null) {
            paths = new TreeSet<>();
            map.put(httpMethod, paths);
        }
        paths.add(path);
    }
}
