
# minimal Message-Driven Microsevervice implementation with Tracing, Circuitbreaker and Sleuth logging meta-data






# dependencies.json for gradle look like this:

```json
{
    "latest": {
        "default": {
            "_comment": "default versions",
            "springBootVersion": "2.1.8.RELEASE",
            "springCloudVersion": "Greenwich.SR2",
            "springJaegerCloud": "2.0.3",
            "jaegerClientVersion": "0.35.5", "_comment1": "latest pre 1.0.0 was 0.35.5",
            "springCloudContractVersion": "2.1.3.RELEASE",
            "logbackVersion": "1.2.3",     "_comment1": "https://github.com/qos-ch/logback/releases",
            "graphqlJavaVersion": "13.0",
            "graphqlSpringVersion": "1.0",
            "snakeYamlVersion": "1.24",
            "braveInstrumentationVersion": "5.6.9",
            "commonsLang3Version": "3.9",
            "picocliVersion": "4.0.1",      "_comment1": "https://github.com/remkop/picocli/releases",
            "junitJupiterVersion": "5.5.2", "_comment1": "https://github.com/junit-team/junit5/releases",
        },
        "snapshots": {
            "_comment": "snapshot dependencies not available in mavenCentral/jcenter",
            "springCloudCircuitbreakerSnapshotVersion": "1.0.0.BUILD-SNAPSHOT",
        },
        "plugin": {
            "_comment": "gradle plugin versions",
            "dependencyManagementPluginVersion": "1.0.8.RELEASE", "_comment1": "https://github.com/spring-gradle-plugins/dependency-management-plugin/releases",
            "propdepsPluginVersion": "0.0.9.RELEASE", "_comment1": "https://github.com/spring-gradle-plugins/propdeps-plugin/releases",
            "cfGradlePluginVersion": "1.1.3",         "_comment1": "to be replaced by https://github.com/pivotalservices/ya-cf-app-gradle-plugin",
            "dockerPluginVersion": "5.0.0",           "_comment1": "https://github.com/bmuschko/gradle-docker-plugin/releases",
            "artifactoryPluginVersion": "latest",     "_comment1": "https://bintray.com/jfrog/jfrog-jars/build-info-extractor-gradle",
            "jenkinsPluginVersion": "1.3.6",          "_comment1": "https://github.com/ghale/gradle-jenkins-plugin/releases",
        },
        "groovy": {
            "_comment": "groovy versions",
            "groovyAllVersion": "2.5.8",
            "spockVersion": "1.3-groovy-2.5"
        },
        "gradle": {
            "_comment": "gradle version",
            "gradleVersion": "5.6.2"
        }
    }
}
```
