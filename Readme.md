
# "minimal" Message-Driven Microsevervice implementation with Monitoring, Tracing, Circuitbreakers and Sleuth logging meta-data downstream

## Synopsis

This project tries to "demo" a message-driven Microservice reference implementation.

`MicroserviceApplication` can be startet as standalone app acting
- as message source (Profile `source`) generating messages every x seconds
- as tier1 processor (Profile `tier1`) consuming messages send by source app and sending to tier2 app
- as tier2 processor (Profile `tier2`) consuming messages send by tier1 app and sending to sind app
- as message sink (Profile `sink`) finally consuming the message and ending the (business) message flow

The app uses the following features and frameworks:
- [spring-cloud-stream](https://spring.io/projects/spring-cloud-stream) \
    Bindings in: ` com/hoffi/minimal/microservices/microservice/bootconfigs/StreamBindingsConfig.java` \
    Channel defs: `com/hoffi/minimal/microservices/microservice/bops/channels/` package
- Messaging with [RabbitMQ](https://www.rabbitmq.com/) and [spring-amqp](https://spring.io/projects/spring-amqp)
- [spring-cloud-circuitbreakers](https://github.com/spring-cloud/spring-cloud-circuitbreaker) via [resilience4j-circuitbreaker](https://resilience4j.readme.io/docs) \
    including `resilience4j-micrometer` and `resilience4j-prometheus` for dashboarding circuitbreaker conditions
- [spring-cloud-sleuth](https://spring.io/projects/spring-cloud-sleuth) and [spring-cloud-zipkin](https://zipkin.io) \
    for Downstream Tracing of Business Processes and logging instrumentation
- Automatic Tracing instrumentation of `spring-cloud-stream` RabbitBinders with [spring-cloud-stream-binder-rabbit](https://cloud.spring.io/spring-cloud-stream-binder-rabbit/)
 
**AND**(!)

- [spring-cloud-contract CDC (Consumer Driven Contracts)](https://spring.io/projects/spring-cloud-contract)

<br/>

Overview Image:
![images/Overview.svg](docSources/images/Overview.svg "Overview image")

<br/>
<br/>

## TL;DR fast start

### compiling

The project is a gradle multi-project (defined in settings.gradle).

The service containing the microservice app is under `microservice`

Versions of dependencies will be read from ./buildSrc/src/main/kotlin/v/dependencies.kt

### starting the applications
a bash script for starting (or dryrun output) all 4 Layer Applications) from within ./microservice/ \
    `microservice/scripts/run_microservices dry`

```
SERVER_PORT=8080 SPRING_PROFILES_ACTIVE=local,sink \
    build/libs/minimal_microservice-0.1.0.RELEASE.jar \
        --spring.application.name=microservice_sink \
        --app.businessLogic.tier=sink \
        --logging.file=microservice_sink.log
SERVER_PORT=8081 SPRING_PROFILES_ACTIVE=local,tier2 \
    build/libs/minimal_microservice-0.1.0.RELEASE.jar \
        --spring.application.name=microservice_tier2 \
        --app.businessLogic.tier=tier2 \
        --logging.file=microservice_tier2.log
SERVER_PORT=8082 SPRING_PROFILES_ACTIVE=local,tier1 \
    build/libs/minimal_microservice-0.1.0.RELEASE.jar \
        --spring.application.name=microservice_tier1 \
        --app.businessLogic.tier=tier1 \
        --logging.file=microservice_tier1.log
SERVER_PORT=8083 SPRING_PROFILES_ACTIVE=local,source \
    build/libs/minimal_microservice-0.1.0.RELEASE.jar \
        --spring.application.name=microservice_source \
        --app.businessLogic.tier=source \
        --logging.file=microservice_source.log
```

### Prerequisits for a local start

#### download and start an openzipkin server:

e.g. by
- curl -sSL https://zipkin.io/quickstart.sh | bash -s
- java -jar zipkin.jar

Access the zipkin UI under `https://<zipkinServerIP>:9411`

#### download and start a rabbitmq-server:

follow instructions on [RabbitMQ Homepage](https://www.rabbitmq.com/), use a dockerized version or install it via your favorite package manager

#### download and install a prometheus server for monitoring:

`./monitoring/run_prometheus_docker.sh` will give you some instructions on how to setup a `dockerized prometheues server`

Access Prometheus server under `http://<prometheusServerIP>:9090/`

Access the Grafana dasboard under `http://<grafanaIP>:3000`


<br/>
<br/>

## Project Layout

```
|-- buildfiles
|-- ci                                          (WiP for concourse pipeline build/install/deploy)
|-- docSources                                  (Documentation md sources)
|-- docs                                        (Generated Documentation pdf/html)
|-- gradle                                      (gradle wrapper dir)
|-- microservice (App sub-project)
|   |-- scripts                                 (helper scripts for App start/demo)
|   `-- src
|       |-- main/java/com/hoffi/minimal/microservices/microservice (App src packages)
|       |   |     |-- bootconfighelpers
|       |   |     |-- bootconfigs               (spring @Configuration classes)
|       |   |     |-- bops                      (bops = BusinessOPerationS = Business code)
|       |   |     |   |-- channels              (spring-cloud-stream channel definitions)
|       |   |     |   |-- inbound               (Sink   (receiving msges) implementations)
|       |   |     |   |-- outbound              (Source (sending msges)   implementations)
|       |   |     |   `-- ws                    (REST webservices/controllers)
|       |   |     |-- businesslogic             (where pure business code resides, without all the boiler plate code)
|       |   |     |-- common
|       |   |     |   `-- dto                   (DTO = DataTransferObject = things send over the wire)
|       |   |     |-- helpers
|       |   |     |-- monitoring                (custom monitoring implementations)
|       |   |     |   |-- annotations           (Annotations for Monitoring)
|       |   |     |   `-- aspects               (AOP implementation for the Monitoring Annotations)
|       |   |     `-- tracing
|       |   `-- resources
|       |       |-- application.yml
|       |       `-- bootstrap.yml
|       `-- test/java/com/hoffi/minimal/microservices/microservice (Test src packages)
|           |   |-- annotations                 (Convenience Annotations for Tagging Tests)
|           |   ... 
|           |   |     |-- bops
|           |   |     |   |-- inbound
|           |   |     |   |-- outbound
|           |   |     |   |   |-- ContractTier1Test.java  (<-- spring-cloud-contract stub-runner test)
|           |   |     |   |   |-- Contract....java        (<-- spring-cloud-contract stub-runner test)
|           |   |     |   |   `-- ...                     (non spring-cloud-contract tests)
|           |   |     |   |-- testhelpers
|           |   |     |   `-- ws                          (REST webservices tests (non-CDC))
|           |   |     |-- businesslogic
|           |   |     |-- common
|           |   |     |-- contractbaseclasses             (spring-cloud-contract producer side base class impls)
|           |   |     |-- helpers
|           |   |     `-- misc
|           |   `-- testhelpers
|           `-- resources
|               |-- contracts                             (CDC (groovy) contracts)
|                   |-- inbound
|                   `-- outbound
|-- monitoring                                            (helper scripts for setting up prometheus/grafana via docker)
|   |-- grafana
|   `-- prometheus
`-- scripts
    |-- monitoring -> ../monitoring
    `-- scripts -> ../microservice/scripts
```

<br/>
<br/>

so all the *\_interesting\_* implementation things reside inside subproject-folder `./microservice/`


<br/>
<br/>

## Birds-Eye-View (or: where to find the interesting code bits in the repo)

`<tbd>`

<br/>
<br/>

## Implementation Features!!!

### spring-cloud-stream messaging between µService Apps

`<tbd>`




<br/>
<br/>

### spring-cloud-circuitbreaker and Resilience4j Circuitbreakers

`<tbd>`




<br/>
<br/>


### Tracing with spring-cloud-sleuth and spring-cloud-zipkin

`<tbd>`




<br/>
<br/>


### Monitoring arbitrary methods via custom Annotations and AOP

`<tbd>`



<br/>
<br/>


### spring-cloud-contract Consumer-Driven-Contracts (CDC) Testing

`<tbd>`



<br/>
<br/>

## miscellaneous stuff

`<tbd>`



<br/>
<br/>

<style type="text/css"> /* automatic heading numbering */ h1 { counter-reset: h2counter; font-size: 24pt; } h2 { counter-reset: h3counter; font-size: 22pt; margin-top: 2em; } h3 { counter-reset: h4counter; font-size: 16pt; } h4 { counter-reset: h5counter; font-size: 14pt; } h5 { counter-reset: h6counter; } h6 { } h2:before { counter-increment: h2counter; content: counter(h2counter) ".\0000a0\0000a0"; } h3:before { counter-increment: h3counter; content: counter(h2counter) "." counter(h3counter) ".\0000a0\0000a0"; } h4:before { counter-increment: h4counter; content: counter(h2counter) "." counter(h3counter) "." counter(h4counter) ".\0000a0\0000a0"; } h5:before { counter-increment: h5counter; content: counter(h2counter) "." counter(h3counter) "." counter(h4counter) "." counter(h5counter) ".\0000a0\0000a0"; } h6:before { counter-increment: h6counter; content: counter(h2counter) "." counter(h3counter) "." counter(h4counter) "." counter(h5counter) "." counter(h6counter) ".\0000a0\0000a0"; } </style>
