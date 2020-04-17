//
// in a multi-project build, this has to be applied from within the build.gradle.kts of the subproject
//
// configurations of interest might be:
// compileClasspath
// runtimeClasspath
// testCompileClasspath
// testRuntimeClasspath

tasks.register("printClasspath") {
    group = "misc"
    description = "print classpath"
    doLast {
        // filters only existing and non-empty dirs
        project.getConfigurations().getByName("compileClasspath").getFiles()
            .filter { (it.isDirectory() && it.listFiles().size > 0) || it.isFile() }
            .forEach{ println(it) }
    }
}

tasks.register("printTestClasspath") {
    group = "misc"
    description = "print tests classpath"
    doLast {
        // filters only existing and non-empty dirs
        project.getConfigurations().getByName("testRuntimeClasspath").getFiles()
            .filter { (it.isDirectory() && it.listFiles().size > 0) || it.isFile() }
            .forEach{ println(it) }
    }
}
