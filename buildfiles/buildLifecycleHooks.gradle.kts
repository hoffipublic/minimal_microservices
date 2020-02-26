
gradle.afterProject {
    if (state.failure != null) {
        println("Evaluation of $project FAILED")
    } else {
        println("Evaluation of $project succeeded")
    }
}
gradle.taskGraph.beforeTask {
    doFirst {
        if(!this.state.getNoSource() || this.state.getUpToDate()) {
            println(">executing ... ")
        }
    }
}
gradle.taskGraph.afterTask {
    if(!this.state.getSkipped()) {
        println("<ready.")
    }
}
