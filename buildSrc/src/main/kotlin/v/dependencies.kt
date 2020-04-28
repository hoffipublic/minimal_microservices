package v

const val MAVEN = "https://repo1.maven.org"
const val MAVEN2 = "maven2"
const val SPRING = "https://repo.spring.io"
const val MS = "milestone"
const val SNAPSHOT = "snapshot"
const val GRADLEPLUGINS = "https://plugins.gradle.org"
const val PLUGIN = "plugin"

// singleton definitions
object Latest {
    enum class Release(val version: String, val repo: String, val path: String, val group: String, val artifact: String) {
        springBoot("2.2.6.RELEASE",           MAVEN, MAVEN2, "org.springframework.boot",     "spring-boot-dependencies"),
        springCloud("Hoxton.SR4",             MAVEN, MAVEN2, "org.springframework.cloud",    "spring-cloud-dependencies"),
        springCloudContract("2.2.2.RELEASE",  MAVEN, MAVEN2, "org.springframework.cloud",    "spring-cloud-starter-contract-verifier"),
        resilience4j("1.4.0",                 MAVEN, MAVEN2, "io.github.resilience4j",       "resilience4j-spring-boot2"),
        logback("1.2.3",                      MAVEN, MAVEN2, "ch.qos.logback",               "logback-core"),
        graphqlJava("14.0",                   MAVEN, MAVEN2, "com.graphql-java",             "graphql-java"),
        graphqlSpring("7.0.1",                MAVEN, MAVEN2, "com.graphql-java-kickstart",   "graphql-spring-boot-starter"),
        snakeYaml("1.26",                     MAVEN, MAVEN2, "org.yaml",                     "snakeyaml"),
        jacksonModuleKotlin("2.10.3",         MAVEN, MAVEN2, "com.fasterxml.jackson.module", "jackson-module-kotlin"),
        braveInstrumentation("5.11.2",        MAVEN, MAVEN2, "io.zipkin.brave",              "brave-instrumentation-parent"),
        commonsLang3("3.10",                   MAVEN, MAVEN2, "org.apache.commons",           "commons-lang3"),
        commonsText("1.8",                    MAVEN, MAVEN2, "org.apache.commons",           "commons-text"),
        picocli("4.2.0",                      MAVEN, MAVEN2, "info.picocli",                 "picocli"),
        junitJupiter("5.6.1",                 MAVEN, MAVEN2, "org.junit.jupiter",            "junit-jupiter")
    }
    enum class Milestone (val version: String, val repo: String, val path: String, val group: String, val artifact: String) {
        springBoot("2.3.0.M4",                SPRING, MS, "org.springframework.boot", "spring-boot-dependencies"),
        springCloud("2020-1.M1",              SPRING, MS, "org.springframework.cloud", "spring-cloud-dependencies"),
        springCloudCircuitbreaker("2.0.0.M1", SPRING, MS, "org.springframework.cloud", "spring-cloud-circuitbreaker-dependencies"),
        springSleuth("3.0.0.M1",              SPRING, MS, "org.springframework.cloud", "spring-cloud-sleuth-dependencies"),
        springCloudContract("3.0.0.M1",       SPRING, MS, "org.springframework.cloud", "spring-cloud-starter-contract-verifier")
    }
    enum class Snapshot(val version: String, val repo: String, val path: String, val group: String, val artifact: String) {
        springBoot("2.3.0.BUILD-SNAPSHOT",    SPRING, SNAPSHOT, "org.springframework.boot", "spring-boot-dependencies"),
        springCloud("",                       SPRING, SNAPSHOT, "org.springframework.cloud", "spring-cloud-dependencies"),
        springCloudCircuitbreaker("1.0.0.BUILD-SNAPSHOT", SPRING, SNAPSHOT, "org.springframework.cloud", "spring-cloud-circuitbreaker-dependencies"),
        springSleuth("3.0.0.M1", "", "", "", ""),
        springCloudContract("3.0.0.M1", "", "", "", "")
    }
    enum class Plugin(val version: String, val repo: String, val path: String, val group: String, val artifact: String) {
        kotlin("1.3.72",                      GRADLEPLUGINS, PLUGIN, "", "org.jetbrains.kotlin.jvm"),
        dependencyManagement("1.0.9.RELEASE", GRADLEPLUGINS, PLUGIN, "", "io.spring.dependency-management"),
        springCloudContract("2.2.2.RELEASE",  GRADLEPLUGINS, PLUGIN, "", "org.springframework.cloud.contract"), 
        propdeps("0.0.9.RELEASE",             "", "", "", ""),
        cfGradle("1.1.3",                     "", "", "", ""),
        docker("6.4.0",                       GRADLEPLUGINS, PLUGIN, "", "com.bmuschko.docker-spring-boot-application"),
        artifactory("4.15.2",                 GRADLEPLUGINS, PLUGIN, "", "com.jfrog.artifactory"), //("latest"),
        jenkins("1.3.6",                      GRADLEPLUGINS, PLUGIN, "", "com.terrafolio.jenkins"),
        gitProperties("2.2.2",                GRADLEPLUGINS, PLUGIN, "", "com.gorylenko.gradle-git-properties"),
        jib("2.2.0",                          GRADLEPLUGINS, PLUGIN, "", "com.google.cloud.tools.jib")
    }
    enum class Groovy(val version: String, val repo: String, val path: String, val group: String, val artifact: String) {
        groovyAll("2.5.11",     MAVEN, MAVEN2, "org.codehaus.groovy",     "groovy-all"), // groovyAll("2.5.11"),
        spock("1.3-groovy-2.5", MAVEN, MAVEN2, "org.spockframework",      "spock-core") // spock("1.3-groovy-2.5")
    }
}

// Extension functions on gradles DependencyHandler class
val springBootLatest = Latest.Release.springBoot.version
val springCloudLatest = Latest.Release.springCloud.version
val springCloudContractLatest = Latest.Release.springCloudContract.version
val resilience4jLatest = Latest.Release.resilience4j.version
val logbackLatest = Latest.Release.logback.version
val graphqlJavaLatest = Latest.Release.graphqlJava.version
val graphqlSpringLatest = Latest.Release.graphqlSpring.version
val snakeYamlLatest = Latest.Release.snakeYaml.version
val jacksonModuleKotlinLatest = Latest.Release.jacksonModuleKotlin.version
val braveInstrumentationLatest = Latest.Release.braveInstrumentation.version
val commonsLang3Latest = Latest.Release.commonsLang3.version
val commonsTextLatest = Latest.Release.commonsText.version
val picocliLatest = Latest.Release.picocli.version
val junitJupiterLatest = Latest.Release.junitJupiter.version

val springCloudSnapshot = Latest.Snapshot.springCloud.version
val springCloudCircuitbreakerSnapshot = Latest.Snapshot.springCloudCircuitbreaker.version
val springSleuthSnapshot = Latest.Snapshot.springSleuth.version
val springCloudContractSnapshot = Latest.Snapshot.springCloudContract.version

val kotlinPlugin = Latest.Plugin.kotlin.version
val dependencyManagementPlugin = Latest.Plugin.dependencyManagement.version
val springCloudContractPlugin = Latest.Plugin.springCloudContract.version
val propdepsPlugin = Latest.Plugin.propdeps.version
val cfGradlePlugin = Latest.Plugin.cfGradle.version
val dockerPlugin = Latest.Plugin.docker.version
val artifactoryPlugin = Latest.Plugin.artifactory.version
val jenkinsPlugin = Latest.Plugin.jenkins.version
val gitPropertiesPlugin = Latest.Plugin.gitProperties.version
val jibPlugin = Latest.Plugin.jib.version

val groovyAllGroovy = Latest.Groovy.groovyAll.version
val spockGroovy = Latest.Groovy.spock.version
