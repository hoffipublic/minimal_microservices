
gradle.afterProject {
    if (state.failure != null) {
        println("Evaluation of $project FAILED")
    } else {
        println("Evaluation of $project succeeded")
    }
}
//gradle.taskGraph.beforeTask {
//    doFirst {
//        if(!this.state.getNoSource() || this.state.getUpToDate()) {
//            println(">... ")
//        }
//    }
//}
gradle.taskGraph.afterTask {
    if(!this.state.getSkipped()) {
        var pname = project.name
        if(pname == project.rootProject.name) {
            pname = ""
        } else {
            pname = ":${pname}"
        }
        println("< Task ${pname}:${this.name} finished.")
    }
}
