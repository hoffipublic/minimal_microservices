package com.hoffi.minimal.microservices.microservice.monitoring.aspects;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.hoffi.minimal.microservices.microservice.businesslogic.BusinessLogic;
import com.hoffi.minimal.microservices.microservice.monitoring.annotations.Monitored;
import com.hoffi.minimal.microservices.microservice.tracing.ChunkScoped;
import com.hoffi.minimal.microservices.microservice.tracing.TracingHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Aspect
@Component
public class MonitoredAspect {
    private static Logger log = LoggerFactory.getLogger(MonitoredAspect.class);
    private static final String GAUGETYPE = "GAUGE";
    private static final String COUNTERTYPE = "COUNTER";
    private static final String TIMERTYPE = "TIMER";

    private String counterName = "methodCalls";
    private String sinceLastName = "timeSinceLastCall";
    private String sincePreviousName = "timeSincePreviousCall";
    private String timerName = "methodDuration";

    @Value("${app.info.instance_index}")
    private String instanceIndex;
    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private AllMetersMap allMetersMap;

    @Autowired
    private TracingHelper tracingHelper;

    public Set<String> initialisedMonitors = ConcurrentHashMap.newKeySet(8);
    
    private long previousCallTime;
    private long lastCallTime;

    /**
     * if Aspect is defined in another package you have to pass the fully qualified Annotation class
     * name
     */
    @Around("@annotation(com.hoffi.minimal.microservices.microservice.monitoring.annotations.Monitored)")
    public Object monitorMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String opName = this.getClass().getSimpleName() + '.' +  new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        Timer.Sample sample = null;
        String[] annotationValueQualified = null;
        try(ChunkScoped chunkScoped = tracingHelper.startChunk(opName)) {
            this.previousCallTime = lastCallTime;
            this.lastCallTime = System.currentTimeMillis();
            annotationValueQualified = getAnnotationValueQualified(joinPoint);
            initOrSkipMonitored(annotationValueQualified);

            getCounter(annotationValueQualified, counterName).increment();
            sample = Timer.start(meterRegistry);
        }

        Object proceed = joinPoint.proceed();

        try(ChunkScoped chunkScoped = tracingHelper.startChunk(opName)) {
            sample.stop(getTimer(annotationValueQualified, timerName));
            getGauge(annotationValueQualified, sinceLastName).value();
            getGauge(annotationValueQualified, sincePreviousName).value();

            log.info("{} executed in {}ms", joinPoint.getSignature(), (System.currentTimeMillis() - this.lastCallTime));
        }
        return proceed;
    }

    /**
     * returned qualified name must be unique not only for all methods of this app, but also for all
     * apps that report to prometheus
     */
    private String[] getAnnotationValueQualified(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        String methodNameSimple = joinPoint.getSignature().getName();
        String fullyQualifiedMethodName = joinPoint.getSignature().getDeclaringTypeName() + "." + methodNameSimple;
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class<?>[] methodParams = methodSignature.getMethod().getParameterTypes();
        Method method = joinPoint.getTarget().getClass().getMethod(methodSignature.getMethod().getName(), methodParams);
        Monitored monitored = method.getAnnotation(Monitored.class);
        String annotationValue = monitored.value();
        return new String[] { String.format("%s.%s.%s", appName, instanceIndex,  annotationValue), fullyQualifiedMethodName, annotationValue, methodNameSimple };
    }

    private String meterName(String[] annotationValueQualified, String meterName, String METERTYPE) {
        return String.format("%s_%s%s_%s", annotationValueQualified[0], meterName, METERTYPE, annotationValueQualified[3]);
    }

    private String key(String[] annotationValueQualified, String meterName, String METERTYPE) {
        return String.format("%s_%s%s_%s", annotationValueQualified[0], meterName, METERTYPE, annotationValueQualified[1]);
    }

    /** init the Meters of this Annotation per annotationValueQualified */
    private void initOrSkipMonitored(String[] annotationValueQualified) {
        if (!initialisedMonitors.add(annotationValueQualified[2]+"_"+annotationValueQualified[1])) {
            return;
        }

        putCounter(annotationValueQualified, counterName,
                Counter.builder(meterName(annotationValueQualified, counterName, COUNTERTYPE))
                        .baseUnit("count").description("This is my counter")
                        .register(meterRegistry));

        putGauge(annotationValueQualified, sinceLastName,
                Gauge.builder(meterName(annotationValueQualified, sinceLastName, GAUGETYPE), this, MonitoredAspect::calcTimeSinceLastCall)
                        .baseUnit("seconds").description("This is the time since last-call")
                        .register(meterRegistry));

        putGauge(annotationValueQualified, sincePreviousName,
                Gauge.builder(meterName(annotationValueQualified, sincePreviousName, GAUGETYPE), this, MonitoredAspect::calcPreviousSinceLastCallTime)
                        .baseUnit("seconds").description("This is the time of last-call minus previous-call")
                        .register(meterRegistry));

        putTimer(annotationValueQualified, timerName,
                 Timer.builder(meterName(annotationValueQualified, timerName, TIMERTYPE))
                        .description("This is the time the method took")
                        .register(meterRegistry));
    }

    private Counter getCounter(String[] annotationValueQualified, String counterName) {
        return allMetersMap.getCounter(key(annotationValueQualified, counterName, COUNTERTYPE));
    }
    private Gauge getGauge(String[] annotationValueQualified, String gaugeName) {
        return allMetersMap.getGauge(key(annotationValueQualified, gaugeName, GAUGETYPE));
    }
    private Timer getTimer(String[] annotationValueQualified, String timerName) {
        return allMetersMap.getTimer(key(annotationValueQualified, timerName, TIMERTYPE));
    }

    private void putCounter(String[] annotationValueQualified, String counterName, Counter counter) {
        allMetersMap.putCounter(key(annotationValueQualified, counterName, COUNTERTYPE), counter);
    }
    private void putGauge(String[] annotationValueQualified, String gaugeName, Gauge gauge) {
        allMetersMap.putGauge(key(annotationValueQualified, gaugeName, GAUGETYPE), gauge);
    }
    private void putTimer(String[] annotationValueQualified, String timerName, Timer timer) {
        allMetersMap.putTimer(key(annotationValueQualified, timerName, TIMERTYPE), timer);
    }

    public Double calcTimeSinceLastCall() {
        if (lastCallTime == 0) {
            return 0d;
        }
        return (System.currentTimeMillis() - lastCallTime) / 1000d;
    }

    public Double calcPreviousSinceLastCallTime() {
        if (previousCallTime == 0) {
            return 0d;
        }
        return (lastCallTime - previousCallTime) / 1000d;
    }

}
