tasks.register("checkUpdates") {
    group = "misc"
    description = "check for new updates to dependancy jars"
    doLast {
        println("checking defined dependency versions in ./buildSrc/src/main/kotlin/v/dependencies.kt")
        println("RELEASEs:")
        for(d in v.Latest.Release.values()) {
            if(d.repo == null || d.repo == "") continue
            val text = java.net.URL("${d.repo}/${d.path}/${d.group.replace('.', '/')}/${d.artifact}/maven-metadata.xml").readText()
            val regexLatest = Regex("<latest>(.*)</latest>", RegexOption.MULTILINE)
            val regexRelease = Regex("<release>(.*)</release>", RegexOption.MULTILINE)
            val regexVersions = Regex(".*<version>(.*)</version>", setOf(RegexOption.MULTILINE, RegexOption.UNIX_LINES))
            var latestMatchResult = regexLatest.find(text)
            var releaseMatchResult = regexRelease.find(text)
            var versionsSet = regexVersions.findAll(text)
            val lastVersion = versionsSet.mapNotNull{ match -> match.groupValues[1] }.findLast{
                ! it.contains(Regex("(alpha|beta|\\d\\d\\d\\d-\\d\\d-\\d\\d)"))
            }

            print(String.format("%-29s: current: %-19s", d.name, d.version))
            var latest = "not found"
            var release = "not found"
            if(latestMatchResult != null) latest = latestMatchResult.groupValues[1]
            if(releaseMatchResult != null) release = releaseMatchResult.groupValues[1]

            if((d.version == release) || (d.version == lastVersion))  {
                print(" up-to-date")
            } else {
                print(String.format(" latest: %-19s release: %-19s lastVersionRef: %-19s", latest.take(19), release.take(19), lastVersion))
            }
            println("")
        }
        println("")
        println("MILESTONEs:")
        for(d in v.Latest.Milestone.values()) {
            if(d.repo == null || d.repo == "") continue
            val text = java.net.URL("${d.repo}/${d.path}/${d.group.replace('.', '/')}/${d.artifact}/maven-metadata.xml").readText()
            val regexLatest = Regex("<latest>(.*)</latest>", RegexOption.MULTILINE)
            val regexRelease = Regex("<release>(.*)</release>", RegexOption.MULTILINE)
            val regexVersions = Regex(".*<version>(.*)</version>", setOf(RegexOption.MULTILINE, RegexOption.UNIX_LINES))
            var latestMatchResult = regexLatest.find(text)
            var releaseMatchResult = regexRelease.find(text)
            var versionsSet = regexVersions.findAll(text)
            val lastVersion = versionsSet.mapNotNull{ match -> match.groupValues[1] }.findLast{
                ! it.contains(Regex("(alpha|beta|\\d\\d\\d\\d-\\d\\d-\\d\\d)"))
            }

            print(String.format("%-29s: current: %-19s", d.name, d.version))
            var latest = "not found"
            var release = "not found"
            if(latestMatchResult != null) latest = latestMatchResult.groupValues[1]
            if(releaseMatchResult != null) release = releaseMatchResult.groupValues[1]

            if((d.version == release) || (d.version == lastVersion))  {
                print(" up-to-date")
            } else {
                print(String.format(" latest: %-19s release: %-19s lastVersionRef: %-19s", latest.take(19), release.take(19), lastVersion))
            }
            println("")
        }
        println("")
        println("Plugins:")
        for(d in v.Latest.Plugin.values()) {
            if(d.repo == null || d.repo == "") continue
            val regexLatest = Regex("Version (.*) \\(latest\\)", setOf(RegexOption.MULTILINE, RegexOption.UNIX_LINES))
            val text = java.net.URL("${d.repo}/${d.path}/${d.artifact}").readText()
            // println(text)
            var latestMatchResult = regexLatest.find(text)
            var latest = "not found"
            if(latestMatchResult != null) latest = latestMatchResult.groupValues[1]
            print(String.format("%-29s: current: %-19s", d.name, d.version))
            if(d.version == latest) {
                print(" up-to-date")
            } else {
                print(String.format(" latest: %-19s", latest))
            }
            println("")
        }
    }
}
