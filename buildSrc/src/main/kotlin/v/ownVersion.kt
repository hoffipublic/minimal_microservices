package v

import java.io.File
import org.gradle.api.GradleException

// ============================================================================
// ========  version of this project  =========================================
// ============================================================================

enum class Version(val versionNumber: String, val versionStatus: String, val versionPublishRepoKey: String) {
    master ("0.1.0", "RELEASE",   "libs-release-local"),
    develop("0.1.0", "NIGHTLY",   "libs-snapshot-local"),
    release("0.1.0", "MILESTONE", "libs-snapshot-local"),
    hotfix ("0.1.0", "HOTFIX",    "libs-release-local"),
    support("0.1.0", "SUPPORT",   "libs-release-local")
}

val gitBranchCommitPair = determineCurrentGitBranchAndLastCommitHash()
val branch = gitBranchCommitPair.first
val commit = gitBranchCommitPair.second

val versionAndStatusTriple = determineVersion(branch)
val versionNumber = versionAndStatusTriple.first
val versionStatus = versionAndStatusTriple.second
val versionRepoKey = versionAndStatusTriple.third
val version = versionNumber + "." + versionStatus


fun determineVersion(branch: String): Triple<String,String,String> {
    try {
        val version: Version = Version.valueOf(branch)
        return Triple(version.versionNumber , version.versionStatus, version.versionPublishRepoKey)
    } catch(e: IllegalArgumentException) {
        return Triple(Version.develop.versionNumber, "FEATURE", "libs-snapshot-local")
    }
    //     springProfilesActive = System.env.SPRING_PROFILES_ACTIVE == null ? ["default"]: System.env.SPRING_PROFILES_ACTIVE.split(',')
}


fun determineCurrentGitBranchAndLastCommitHash(): Pair<String,String> {
    var gitBranch = "master"
    var gitCommit = "none"

    val gitFolderName = ".git/"
    val gitFolder = File(gitFolderName)
    val gitHead = File(gitFolder, "HEAD")
    if (gitFolder.exists() && gitFolder.isDirectory() && gitHead.exists()) {
	    /* '.git/HEAD' contains either
	     * in case of detached head: the currently checked out commit hash
	     * otherwise: a reference to a file containing the current commit hash */
	    val head = gitHead.readText().split(":") // .git/HEAD
	    val isCommit = head.size == 1 // e5a7c79edabbf7dd39888442df081b1c9d8e88fd
	    // def isRef = head.length > 1     // ref: refs/heads/master
	
	    if(isCommit) {
	        gitCommit = head[0].trim().take(12) // e5a7c79edabb
	        gitBranch = gitCommit
	    } else {
	        val refHead = File(gitFolderName + head[1].trim()) // .git/refs/heads/master
	        if (refHead.exists()) {
		        gitCommit = refHead.readText().trim().substring(0, 12)
		        val s = head[1].trim()
		        // everything after the second / up to the end with slashes replaced by underscore
		        gitBranch = s.substring(s.indexOf('/', s.indexOf('/') + 1) + 1).replace('/', '_')
		    }
	    }
    }
    val returnPair = Pair(gitBranch,gitCommit)
    println("currentGitBranch: '${returnPair.first}' (commit: ${returnPair.second})")
    if (returnPair.first == "") {  throw GradleException("cannot determine local git branch")  }
    return returnPair

}

