import org.springframework.cloud.contract.verifier.config.TestFramework
import com.google.cloud.tools.jib.gradle.JibExtension

group = project.rootProject.group
version = project.rootProject.version
val env: String by project.rootProject.extra
val artifactName by extra { "minimal_microservice" }
val archivesBaseName by extra { "minimal_microservice" }

apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildMisc.gradle.kts")

spring.loadSpringAppConfigs(project.name,
        projectDir.toString() + "/src/main/resources/application.yml",
        projectDir.toString() + "/src/main/resources/bootstrap.yml"
)
spring.loadEnvConfigs(env, project.name,
        project.rootProject.projectDir.toString() + "/environments.yml"
)
println("")
println("some spring config properties examples:")
println("application.yml property 'spring.sleuth.baggage.correlation-fields' = '${spring.getSpringAppConfig(project.name, "spring.sleuth.baggage.correlation-fields")}'")
println("environment.artifactory.url = '${spring.getEnvConfig(project.name, "environment.artifactory.url")}'")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor") // for @ConfigurationProperties, make sure compileJava.dependsOn(processResources)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter")

    implementation("org.springframework.cloud:spring-cloud-loadbalancer")

    implementation("org.springframework.boot:spring-boot-starter-aop")
    // implementation("io.github.resilience4j:resilience4j-spring-boot2:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-spring-cloud2:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-timelimiter:${v.resilience4jLatest}")

    implementation("io.github.resilience4j:resilience4j-micrometer:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-prometheus:${v.resilience4jLatest}")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // // implementation("org.springframework.cloud:spring-cloud-starter-sleuth' // if using without zipki")
    implementation("org.springframework.cloud:spring-cloud-starter-zipkin")
    implementation("org.springframework.cloud:spring-cloud-sleuth-zipkin")
    

    implementation("org.springframework.cloud:spring-cloud-stream")
    //implemenentation "org.springframework.cloud:spring-cloud-starter-stream-rabbit"
    implementation("org.springframework.amqp:spring-rabbit")
    implementation("io.zipkin.brave:brave-instrumentation-spring-rabbit")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-rabbit")


    // spring-boot-starter-test already includes the following provided libraries
    // AssertJ
    // Hamcrest
    // Mockito
    // JSONassert
    // JsonPath
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    // deprecated since 3.0
//    testImplementation("org.springframework.cloud:spring-cloud-stream-test-support") // messaging test support for spring-cloud-stream
    testImplementation("org.springframework.cloud", "spring-cloud-stream", classifier = "test-binder")
    // spring-cloud-contract
    // API implementor (procucer) side
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
    // API consumer (client) side
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")

    // testImplementation(localGroovy()) // use gradles internal version of groovy
    testImplementation("org.codehaus.groovy:groovy-all:${v.groovyAllGroovy}")
    testImplementation("org.spockframework:spock-core:${v.spockGroovy}")
//    testCompile("de.demo:minimal_microservices:${version}:stubs") {
//        transitive = false
//    }
}

apply(plugin = "maven-publish")
apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildK8s.gradle.kts")
//apply(plugin = "com.terrafolio.jenkins")

// //apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildDocker.gradle")
// //apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildJenkins.gradle")
// apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildVscode.gradle")

// Starting from Gradle 6.2, Gradle performs a sanity check before uploading, to make sure you don’t
// upload stale files (files produced by another build). This introduces a problem with Spring Boot
// applications which are uploaded using the components.java component:
// Artifact my-application-0.0.1-SNAPSHOT.jar wasn't produced by this build.
// This is caused by the fact that the main jar task is disabled by the Spring Boot application, and the
// component expects it to be present. Because the bootJar task uses the same file as the main jar task by default,
configurations {
    val jar by tasks.existing
    val bootJar by tasks.existing
    listOf(apiElements, runtimeElements).forEach { provider ->
        provider.get().outgoing.artifacts.removeIf {
            it.buildDependencies.getDependencies(null).contains(jar)
        }
//        provider.get().outgoing.artifact(bootJar.get())
        println(String.format("%s/libs/%s-%s.jar", project.buildDir, archivesBaseName, version))
        provider.get().outgoing.artifact(File(String.format("%s/libs/%s-%s.jar", project.buildDir, archivesBaseName, version)))
    }
}
configure<org.gradle.api.publish.PublishingExtension> {
    // gradle publishToMavenLocal
    // find ~/.m2/repository -type f -name '*microservice*' | xargs ls -lh | sed -E 's/^(.*) \/.*\.m2\/repository\/(.*)/\1 \2/'
    publications {
        val archivesBaseName: String by project.extra
        val theVersion = version as String
        create<MavenPublication>("maven") {
            groupId = project.rootProject.group as String?
//            artifactId = archivesBaseName
            version = theVersion
            from(components["java"])
            afterEvaluate {
                artifactId = tasks.bootJar.get().archiveBaseName.get()
            }
        }
        create<MavenPublication>("stubs") {
            groupId = project.rootProject.group as String?
//            artifactId = archivesBaseName
            version = theVersion
            val verifierStubsJar by tasks.existing
            artifact(verifierStubsJar.get())
//            println(String.format("%s/libs/%s-%s-stubs.jar", project.buildDir, archivesBaseName, theVersion))
//            artifact(File(String.format("%s/libs/%s-%s-stubs.jar", project.buildDir, archivesBaseName, theVersion)))
            afterEvaluate {
                artifactId = tasks.bootJar.get().archiveBaseName.get()
            }

        }
    }
    //   repositories {
    //        maven {
    //            credentials { ... }
    //
    //            if (project.version.endsWith("-SNAPSHOT"))
    //                url "http://localhost:8081/artifactory/libs-snapshot-local"
    //            else
    //                url "http://localhost:8081/artifactory/libs-release-local"
    //        }
    //    }

}
val build by tasks.existing {
    val publishToMavenLocal by tasks.existing
    dependsOn(publishToMavenLocal) // push jars to mavenLocal after build
}

//whatIsPublished(group:publishing, description:"what is published to maven repo") {
//   doLast {
//   find /Users/hoffmd9/.m2/repository -type f -name '*minimal_microservicess*' | xargs ls -l
//   }
//}
//publishToMavenLocal.finalizedBy whatIsPublished
//publishPubNamePublicationToMavenLocal.finalizedBy whatIsPublished

// contracts {
configure<org.springframework.cloud.contract.verifier.plugin.ContractVerifierExtension> {
    setTestFramework(TestFramework.JUNIT5)
//    val archivesBaseName: String by project.extra
//    contractDependency {
//        setGroupId(project.group as String?)
//        setArtifactId(archivesBaseName)
//        setVersion(project.version)
//    }
    baseClassForTests.set("com.hoffi.minimal.microservices.microservice.contractbaseclasses.DefaultBaseClass")
    baseClassMappings {
        // matching path below src/test/resources/contracts/
		baseClassMapping(".*inbound.*", "com.hoffi.minimal.microservices.microservice.contractbaseclasses.SinkBaseClass")
		baseClassMapping(".*outbound.*", "com.hoffi.minimal.microservices.microservice.contractbaseclasses.SourceBaseClass")
	}
    // // Creates an array with static imports that should be included in generated tests(for example ["org.myorg.Matchers.*"]). By default, it creates an empty array
    // staticImports.set(listOf("org.myorg.Matchers.*"))
    // // the following properties are used when you want to provide where the JAR with contract lays
    // contractDependency {
    //     stringNotation.set("")
    // }

    // contractsPath.set("")
}
// configurations {
//     contracts {
//         transitive = false
//     }
// }

val compileJava by tasks.existing {
    val processResources by tasks.existing
    dependsOn(processResources)
}

//configure<SpringBoot> { // have to use this form, if imported by apply(plugin = "") instead of plugins { id( "" ) }
//configure<org.springframework.boot.gradle.dsl.SpringBootExtension> {
springBoot {
    mainClassName = "com.hoffi.minimal.microservices.microservice.MicroserviceApplication"

    // This statement tells the Gradle Spring Boot plugin to generate a file
    // build/resources/main/META-INF/build-info.properties
    // that is picked up by Spring Boot to display via actuator /info endpoint.
    buildInfo()
    // buildInfo {
    //     properties {
    //         // Generate extra build info.
    //         additional = additionalBuildInfo(project) // Map<String, String>
    //     }
    // }
}

tasks {
    // jar {
    withType<org.gradle.api.tasks.bundling.AbstractArchiveTask> {
        val archivesBaseName: String by project.extra
        archiveBaseName.set(archivesBaseName)

        // here so that also spring-cloud-contract -stubs.jars are copied to root
        val copyLibsToRootProject by rootProject.tasks.existing
        val branchVersionsPrint by rootProject.tasks.existing
        val versionsPrint by project.tasks.existing
        finalizedBy(copyLibsToRootProject, branchVersionsPrint, versionsPrint)
    }

    // bootJar {
    withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
        // launchScript() // make the fat.jar executable (e.g. for sudo ln -s /var/myapp/myapp.jar /etc/init.d/myapp)
        // versions are defined by mapping of git branches to versions in ./buildSrc/src/main/kotlin/v/ownVersion.kt
        val archivesBaseName: String by project.extra
        archiveBaseName.set(archivesBaseName)
        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = v.version
            attributes["provider"] = "gradle"
        }
        doLast {
            val artifactName: String by project.extra
            val rootProjectPath = project.rootProject.projectDir.getPath()
            var subProjectPath = project(":${project.name}").projectDir.getPath()
            subProjectPath = "." + subProjectPath.substring(rootProjectPath.length)
            println("${project.rootProject.group}:${artifactName}:${v.versionNumber} ==> ${subProjectPath}/${relativePath(archiveFile)}")
        }
    }

    // test {
    withType<Test> {
        // Once we call ./gradlew generateContractTests
        // the Spring Cloud Contract Gradle plugin will now generate a JUnit test in the folder build/generated-test-sources
        // and ./gradlew publishToMavenLocal
        useJUnitPlatform {
    //        includeTags "fast"
    //        excludeTags "app", "integration", "messaging", "slow", "trivial"
        }
        failFast = false
        ignoreFailures = true
        // reports.html.enabled = false

        testLogging {
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            events = setOf(
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED) //, STARTED //, standardOut, standardError)
        }

        addTestListener(object : org.gradle.api.tasks.testing.TestListener {
            override fun beforeTest(descriptor: org.gradle.api.tasks.testing.TestDescriptor?) {
                logger.lifecycle("Running " + descriptor)
            }
            override fun beforeSuite(p0: org.gradle.api.tasks.testing.TestDescriptor?) = Unit
            override fun afterTest(desc: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) = Unit
            override fun afterSuite(desc: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) {
                println("\nTest Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
                if (desc.parent != null) { // will match the outermost suite
                    // val out = project.services.get(StyledTextOutputFactory::class.java).create("an-output")
                    // if (result.failedTestCount <= 0) {
                    //     out.withStyle(StyledTextOutput.Style.Success).println("\nTest Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
                    // } else {
                    //     out.withStyle(StyledTextOutput.Style.Failure).println("\nTest Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
                    // }
                }
            }
        })
    }

    withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
        builder = "cloudfoundry/cnb:bionic"
        val archivesBaseName: String by project.extra
        var dockerRegistry = "${spring.getEnvConfig(project.name, "environment.docker.registry")}"
        if (dockerRegistry.length > 0) {
            dockerRegistry += "/"
        }
        imageName = dockerRegistry + archivesBaseName.replace('_', '-')  + ":" + v.versionNumber
    }

    // clean {
    withType<Delete> {
        doFirst {
            // delete "~/.m2/repository/com/example/http-server-dsl-gradle"
        }
    }
}

// withType<com.google.cloud.tools.jib.gradle.JibTask> {
configure<JibExtension> {
        from {
            image = "openjdk:11.0-jre-slim"
        }
        to {
            var dockerRegistry = "${spring.getEnvConfig(project.name, "environment.docker.registry")}"
            if (dockerRegistry.length > 0) {
                dockerRegistry += "/"
            }
            image = "${dockerRegistry}${archivesBaseName.replace('_', '-')}:${v.versionNumber}"
            credHelper = "osxkeychain"
            tags = kotlin.collections.setOf("latest")
        }
        container {
            labels = kotlin.collections.mapOf("maintainer" to "Dirk.Hoffmann@dell.com", "key2" to "value2")
            format = com.google.cloud.tools.jib.api.buildplan.ImageFormat.OCI
            //args = listOf("some", "args")
            ports = kotlin.collections.listOf("8080")
            //jvmFlags = listOf("-Xms512m", "-Xdebug", "-Xmy:flag=jib-rules")
            mainClass = "demo.MinimalApplication"
            creationTime = "USE_CURRENT_TIMESTAMP"
        }
}

