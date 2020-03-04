import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.internal.logging.text.StyledTextOutput.Style

plugins {
    id("eclipse")
    id("idea")
    id("org.springframework.boot") version "${v.springBootLatest}" apply false
    // id("org.springframework.boot") apply false // if using milestone or snapshots use this line instead of above and configure version in ./settings.gradle.kts
    id("org.springframework.cloud.contract") version "${v.springCloudContractLatest}" apply false
    id("com.gorylenko.gradle-git-properties") version "${v.gitPropertiesPlugin}" apply false
    // id("maven-publish") // needed for com.jfrog.artifactory
}

apply(from = "buildfiles/buildLifecycleHooks.gradle.kts")

// for Maven POM generation
group = "demo"
// versions are defined by mapping of git branches to versions in ./buildSrc/src/main/kotlin/v/ownVersion.kt
version = v.version
apply(from = "buildExt.gradle.kts")
val artifactName by extra { project.name }
val archivesBaseName by extra { project.name }


//apply(from = project.rootProject.projectDir.toString() + "/buildfiles/buildDocs.gradle")

println("\n====================================================================================================")
println("RootProject: ${project.rootProject.group}:${artifactName}:${project.rootProject.version}" + c.lastThreeDirsOf(projectDir))
println("====================================================================================================")

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

    // ################################################################################################
    // #####    pure informational stuff on stdout    #################################################
    // ################################################################################################
    tasks.register("versionsPrint") {
        group = "misc"
        description = "extract spring boot versions from dependency jars"
        doLast {
            println("Gradle version: " + project.getGradle().getGradleVersion())
            // println("Groovy version: " + GroovySystem.getVersion())
            println("javac  version: " + org.gradle.internal.jvm.Jvm.current()) // + " with compiler args: " + options.compilerArgs)
            println("versions of core dependencies:")
            val regex = Regex(pattern = "^(spring-cloud-starter|spring-boot-starter)-[0-9].*$")
            project.configurations.findByName("compileClasspath")?.map { it.name }?.filter { it.matches(regex) }
                    ?.forEach { println("  " + it.replace(".jar", "")) }
        }
    }
//    val build by tasks.existing {
//        val versionsPrint by tasks.existing
//        dependsOn(versionsPrint)
//    }
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

// ################################################################################################
// #####    pure informational stuff on stdout    #################################################
// ################################################################################################
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

// ################################################################################################
// #####    faked (java) tasks for rootProject    #################################################
// ################################################################################################

// rootProject is no java project, so we "fake" a copy task for it after all subprojects.jars
// to copy the subproject jars to rootProject.buildDir/libs
val copyLibsToRootProject = tasks.register<Copy>("copyLibsToRootProject") {
    val libsDirs: MutableList<String> = mutableListOf()
    subprojects.forEach { libsDirs.add("${it.buildDir}/libs") }

    from(libsDirs)
    into(project.rootProject.buildDir.toString() + "/libs")
}
// build.finalizedBy publishToMavenLocal // push jars to mavenLocal after build

// rootProject is no java project, so we "fake" the build task for it after all subprojects.build
// to copy the subproject jars to rootProject.buildDir/libs
// and give additional version information
tasks.register("build") {
//    finalizedBy(copyLibsToRootProject)
}


// rootProject is no java project, so we "fake" the clean task for it
// so all subproject.clean tasks run and this one, if called from rootProject
tasks.register<Delete>("clean") {
    description = "clean jars from from rootProject and subprojects"
    // delete(fileTree(project.buildDir).include("**/*"))
    delete(project.buildDir)
}
