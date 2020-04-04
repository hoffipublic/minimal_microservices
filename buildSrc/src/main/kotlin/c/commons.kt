package c

import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

fun utctimepostfix(): String {
     return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toString().replace(":", "")
}

fun lastThreeDirsOf(dir: File): String {
    val regex = Regex("/?(([^/]+)?/?([^/]+)?/?([^/]+)?/)(?:[^/]+)$")
    val matchResults = regex.find(dir.toString())?.destructured?.toList()
    var lastThreeDirs = " (.../"
    lastThreeDirs += matchResults?.get(0) + ")"
    return lastThreeDirs
}
