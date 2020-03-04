rootProject.name = "minimal_microservices"

include("microservice")

// use this block if using plugins via apply(plugin = "") instead of plugins { id( "" ) }
// e.g. if having to use milestone or snapshot releases not available in the standard repos
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        jcenter()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
        maven("https://repo.spring.io/plugins-release")
        jcenter()
    }
//    resolutionStrategy {
//        eachPlugin {
//            if (requested.id.id == "org.springframework.boot") {
//                val springBootVersion = "2.3.0.M2"
//                println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
//                println("./settings.gradle.kts resoutionStrategy { \"org.springframework.boot\" } ==> spring-boot-gradle-plugin:${springBootVersion}")
//                println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
//                useModule("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
//            }
//        }
//    }
}
