val artifactName by extra { project.name.toLowerCase() }
val archivesBaseName by extra { project.name.toLowerCase() }
val repo by extra("git@gitlab.com:hoffi_minimal/minimal_microservices.git")
val repoHttps by extra("https://gitlab.com/hoffi_minimal/minimal_microservices")

// override on commandline by $ ./gradlew -Penv=yourenvname
val env by extra {
    try { project.property("env") }
    catch(ex: groovy.lang.MissingPropertyException) { "local" }
}