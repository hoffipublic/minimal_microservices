package com.hoffi.minimal.microservices.microservice.bootconfigs;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.jaegertracing.internal.Constants;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.metrics.InMemoryMetricsFactory;
import io.jaegertracing.internal.reporters.CompositeReporter;
import io.jaegertracing.internal.reporters.LoggingReporter;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import io.opentracing.contrib.java.spring.jaeger.starter.TracerBuilderCustomizer;

// activating a io.opentracing.Tracer if opentracing.jaeger.enabled is set to false. This tracer is
// necessary to keep the various Spring configurations happy but has been configured to not sample
// any requests, therefore effectively disabling tracing
//@ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "false", matchIfMissing = false)
@Configuration
public class JaegerConfig {

    /**
     * If arbitrary customizations need to be performed on Tracer.Builder but you don't want to
     * forgo the rest of the auto-configuration features, TracerBuilderCustomizer comes in handy. It
     * allows the developer to invoke any method of Tracer.Builder (with the exception of build)
     * before the auto-configuration code invokes the build method.
     */
    public class ExpandExceptionLogsTracerBuilderCustomizer implements TracerBuilderCustomizer {

        @Override
        public void customize(JaegerTracer.Builder builder) {
            builder.withExpandExceptionLogs();
        }
    }

    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        Map<String, String> tags = new HashMap<>();
        tags.put(Constants.TRACER_HOSTNAME_TAG_KEY, "localhost");

        CompositeReporter reporter = new CompositeReporter(new LoggingReporter(), remoteReporter());


        return new JaegerTracer.Builder("myTestSpringApp").withSampler(new ConstSampler(true))
                .withMetricsFactory(new InMemoryMetricsFactory()).withReporter(reporter)
                .withTags(tags).build();
    }

    @Bean
    public RemoteReporter remoteReporter() {
        return new RemoteReporter.Builder().withSender(new UdpSender("localhost", 6831, 0)).build();
    }
}
