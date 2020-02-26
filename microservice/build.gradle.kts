import org.springframework.cloud.contract.verifier.config.TestFramework

group = project.rootProject.group
version = project.rootProject.version
val artifactName by extra { "minimal_microservice" }
val archivesBaseName by extra { "minimal_microservice" }

spring.loadSpringAppConfigs(project.name,
                            projectDir.toString() + "/src/main/resources/application.yml",
                            projectDir.toString() + "/src/main/resources/bootstrap.yml",
                            project.rootProject.projectDir.toString() + "/environments.yml")
println("application.yml property 'spring.sleuth.baggage-keys' = '${spring.getSpringConfig(project.name, "spring.sleuth.baggage-keys")}'")
println("environment.artifactory.url = '${spring.getSpringConfig(project.name, "environment.artifactory.url")}'")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor") // for @ConfigurationProperties, make sure compileJava.dependsOn(processResources)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter")

    implementation("org.springframework.boot:spring-boot-starter-aop")
    // implementation("io.github.resilience4j:resilience4j-spring-boot2:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-spring-cloud2:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-timelimiter:${v.resilience4jLatest}")

    implementation("io.github.resilience4j:resilience4j-micrometer:${v.resilience4jLatest}")
    implementation("io.github.resilience4j:resilience4j-prometheus:${v.resilience4jLatest}")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus:${v.micrometerRegistryPrometheusLatest}")

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
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-support") // messaging test support for spring-cloud-stream
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
apply(plugin = "com.jfrog.artifactory")
//apply(plugin = "com.terrafolio.jenkins")

// apply(plugin = "cloudfoundry")
// //apply plugin: "docker"
// apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildCf.gradle")  // showCfTargetAndLogin, showCfCmds and generateCfCmds
// //apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildDocker.gradle")
// apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildArtifactory.gradle")
// //apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildJenkins.gradle")
apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildMisc.gradle.kts") // showBootRunCommand
// apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildVscode.gradle")

// publishing {
//     // gradle publishToMavenLocal
//     // find ~/.m2/repository -type f -name '*microservices*' | xargs ls -lh | sed -E 's/^(.*) \/.*\.m2\/repository\/(.*)/\1 \2/'
//     publications {
//         maven(MavenPublication) {
//             groupId = groupId
//             artifactId = project.ext.archiveBaseName
//             version = version

//             from components.java
//         }
// 		stubs(MavenPublication) {
// 			//artifactId "${archiveBaseName}-stubs"
// 			artifactId project.ext.archiveBaseName
// 			artifact verifierStubsJar
// 		}
//     }
//     //   repositories {
//     //        maven {
//     //            credentials { ... }
//     //
//     //            if (project.version.endsWith("-SNAPSHOT"))
//     //                url "http://localhost:8081/artifactory/libs-snapshot-local"
//     //            else
//     //                url "http://localhost:8081/artifactory/libs-release-local"
//     //        }
//     //    }
    
// }
// build.finalizedBy publishToMavenLocal // push jars to mavenLocal after build

// whatIsPublished(group:publishing, description:"what is published to maven repo") {
//    doLast {
//    find /Users/hoffmd9/.m2/repository -type f -name '*minimal_microservicess*' | xargs ls -l
//    }
//}
//publishToMavenLocal.finalizedBy whatIsPublished
//publishPubNamePublicationToMavenLocal.finalizedBy whatIsPublished

// contracts {
configure<org.springframework.cloud.contract.verifier.plugin.ContractVerifierExtension> {
    setTestFramework(TestFramework.JUNIT5)
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

// compileJava {
tasks.withType<JavaCompile> {
    doFirst {
        // options.compilerArgs += ["--add-modules", "java.xml.bind"]
        println("Gradle version: " + project.getGradle().getGradleVersion())
        // println("Groovy version: " + GroovySystem.getVersion())
        println("javac  version: " + org.gradle.internal.jvm.Jvm.current() + " with compiler args: " + options.compilerArgs)
        println("boot   version: " + v.springBootLatest)
        println("cloud  version: " + v.springCloudLatest)
        println("")
    }
}
//compileTestJava {
//    // options.compilerArgs += ["--add-modules", "java.xml.bind"]
//  println 'compileTestJava with version ' + org.gradle.internal.jvm.Jvm.current() + ' with compiler args: ' + options.compilerArgs
//}

// springBoot {
configure<org.springframework.boot.gradle.dsl.SpringBootExtension> {
    mainClassName = "com.hoffi.minimal.microservices.microservice.MicroserviceApplication"

    // This statement tells the Gradle Spring Boot plugin to generate a file
    // build/resources/main/META-INF/build-info.properties
    // that is picked up by Spring Boot to display via actuator /info endpoint.
    buildInfo()
    // buildInfo {
    //     properties {
    //         // Generate extra build info.
    //         additional = additionalBuildInfo(project) // from buildfiles/buildMisc.gradle
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
        val springBootVersionsPrint by rootProject.tasks.existing
        finalizedBy(copyLibsToRootProject, branchVersionsPrint, springBootVersionsPrint)
    }

    // bootJar {
    withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
        launchScript() // make the fat.jar executable (e.g. for sudo ln -s /var/myapp/myapp.jar /etc/init.d/myapp)
        // versions are defined by mapping of git branches to versions in ./buildSrc/src/main/kotlin/v/ownVersion.kt
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

    // clean {
    withType<Delete> {
        doFirst {
            // delete "~/.m2/repository/com/example/http-server-dsl-gradle"
        }
    }
}
// bootRun { systemProperties = System.properties } // ensure Gradle passes command line arguments to the JVM


