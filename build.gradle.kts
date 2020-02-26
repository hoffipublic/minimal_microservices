import org.gradle.internal.logging.text.StyledTextOutput 
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.internal.logging.text.StyledTextOutput.Style

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        maven { url = uri("https://repo.spring.io/plugins-release") }
        maven { url = uri("https://repo.jenkins-ci.org/public/") } // only if using buildfiles/buildJenkins.gradle
    }
    dependencies {
        classpath("gradle.plugin.org.springframework.cloud:spring-cloud-contract-gradle-plugin:${v.springCloudContractLatest}")
        classpath("io.spring.gradle:propdeps-plugin:${v.propdepsPlugin}") // https://github.com/spring-gradle-plugins/propdeps-plugin/releases
    }
}

plugins {
    id("eclipse")
    id("idea")
    id("org.springframework.boot") version "${v.springBootLatest}" apply false
    id("com.gorylenko.gradle-git-properties") version "${v.gitPropertiesPlugin}" apply false
    // // id("maven-publish") // needed for com.jfrog.artifactory
    id("com.jfrog.artifactory") version "${v.artifactoryPlugin}" apply false
}

apply(from = "buildfiles/buildLifecycleHooks.gradle.kts")

// for Maven POM generation
group = "demo"
// versions are defined by mapping of git branches to versions in ./buildSrc/src/main/kotlin/v/ownVersion.kt
version = v.version
val artifactName by extra { project.name }
val archivesBaseName by extra { project.name }


//apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildDocs.gradle")

println("================================================================================")
println("RootProject Group: ${project.rootProject.group}:${artifactName}:${project.rootProject.version}" + c.lastThreeDirsOf(projectDir))
println("================================================================================")

gradle.projectsEvaluated {
    println("\nEvaluation of all multiproject gradle buildfiles completed\n")
}

// tasks.withType<KotlinCompile> {
//     kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString() // VERSION_1_8
// }

subprojects {
    // if you use the imperative apply() function to apply a plugin, then you will have to use the configure<T>() function to configure that plugin. 
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "org.springframework.cloud.contract")
    apply(plugin = "com.gorylenko.gradle-git-properties")
    apply(plugin = "propdeps")
    apply(plugin = "propdeps-eclipse")
    apply(plugin = "propdeps-idea")

    repositories {
        mavenLocal()
        jcenter()
        maven { url = uri("https://repo.spring.io/milestone") } // only if using milestone spring stuff
        maven { url = uri("https://repo.spring.io/libs-snapshot") } // only if using snapshot spring stuff
    }

    // if a plugin is not applied via plugin { id(...) }
    // but via apply(plugin = "")
    // the build script can not use type-safe accessors in this case because the apply() call happens in the body of the build script
    // dependencyManagement {
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${v.springCloudLatest}")
            mavenBom("org.springframework.cloud:spring-cloud-contract-dependencies:${v.springCloudContractLatest}")
        }
    }

    tasks.withType<JavaCompile> {
        //sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        //targetCompatibility = JavaVersion.VERSION_1_8.toString()
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
        doFirst {
            println("> compileJava: ${project.name}")
        }
    }
}

tasks.register("branchVersionsPrint") {
    group = "misc"
    description = "versions are defined by mapping of git branches to versions in ./buildSrc/src/main/kotlin/v/ownVersion.kt"

    doLast {
        println("git branch to version status from ./buildSrc/src/main/kotlin/v/devendencies.kt:")
        enumValues<v.Version>().forEach { println("${it.name.padStart(7, ' ')}: ${it.versionNumber} ${it.versionStatus.padStart(9, ' ')} ") }
        println("\ncurrent git branch was ${v.branch} (commit: ${v.commit}) --> jar postfix: ${v.version}.jar")
        exec {
            commandLine = listOf("git", "status", "--porcelain")
        } 
    }
}


// rootProject is no java project, so we "fake" the build task for it after all subprojects.build
// to copy the subproject jars to rootProject.buildDir/libs
val copyLibsToRootProject = tasks.register<Copy>("copyLibsToRootProject") {
    val libsDirs: MutableList<String> = mutableListOf()
    subprojects.forEach { libsDirs.add("${it.buildDir}/libs") }

    from(libsDirs)
    into(project.rootProject.buildDir.toString() + "/libs")
}

tasks.register("springBootVersionsPrint") {
    group = "misc"
    description = "extract spring boot versions from dependency jars"
    doLast {
        // project.configurations.compile.each { println it.name }
        val regex = Regex(pattern = "^(spring-cloud-starter|spring-boot-starter)-[0-9].*$")
        val aSet: MutableSet<String> = mutableSetOf()
        subprojects.forEach { sp ->
            sp.getConfigurations().getByName("compileClasspath").getFiles().map{it.name}.filter{it.matches(regex)}
                .forEach{aSet.add(String.format("%-25s: %s", project.name, it.replace(".jar", "")))}
        }
        aSet.toSortedSet().forEach{println(it)}
    }
}

// rootProject is no java project, so we "fake" the clean task for it
// so all subproject.clean tasks run and this one, if called from rootProject
tasks.register<Delete>("clean") {
    description = "clean jars from from rootProject and subprojects"
    // delete(fileTree(project.buildDir).include("**/*"))
    delete(project.buildDir)
}
