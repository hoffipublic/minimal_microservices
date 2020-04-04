package spring

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader
import java.util.LinkedList
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.Map
import org.gradle.api.GradleException

// singleton definitions
object SpringConfigStoreByName {
    val springConfigsByName = LinkedHashMap<String, SpringProjectConfig>()

    class SpringProjectConfig {
        val configFilesDefined = LinkedHashSet<String>()
        val profilesActive = LinkedHashSet<String>()
        val profileNames = LinkedHashSet<String>()
        val otherProfileNames = LinkedHashSet<String>()
        val springConfigValuesByProfile = LinkedHashMap<String, Map<String, Any>>()
    }

    fun getOrCreate(projectName: String): SpringProjectConfig {
        var config = springConfigsByName[projectName]
        if(config == null) {
            config = SpringProjectConfig()
            springConfigsByName[projectName] = config
        }
        return config
    }

    fun getMapByProfile(projectName: String, profile: String): Map<String, Any> {
        var config: SpringProjectConfig? = springConfigsByName[projectName]
        if(config == null) {
            throw GradleException("no spring configs read by gradle (sub)project ${projectName}")
        }
        val map = config.springConfigValuesByProfile[profile]
        if(map == null) {
            throw GradleException("project: '${projectName}' has no values for profile '${profile}' defined!")
        }
        return map
    }
}

fun getSpringConfig(projectName: String, keyList: List<String>): String {
    val values: MutableSet<String> = mutableSetOf()
    for(activeProfile in SpringConfigStoreByName.getOrCreate(projectName).profilesActive) {
        val sofar: MutableList<String> = mutableListOf()
        values.addAll(getValueOfCompoundKeyRecursion(projectName, 0, sofar, keyList, SpringConfigStoreByName.getMapByProfile(projectName, activeProfile)))
    }
    if(values.size == 0) {
        val sofar: MutableList<String> = mutableListOf()
        values.addAll(getValueOfCompoundKeyRecursion(projectName, 0, sofar, keyList, SpringConfigStoreByName.getMapByProfile(projectName, "default")))
    }
    if(values.size == 1) {
        return values.elementAt(0)
    }
    if(values.size > 1) {
        throw GradleException("project: '${projectName}' has ambiguous values for springConfigValue ${keyList.joinToString(".")}:'${values.joinToString("','")}' in active spring profiles ${SpringConfigStoreByName.getOrCreate(projectName).profilesActive}")
    }
    throw GradleException("no values found for springConfigValue '${keyList.joinToString(".")}' neither in active spring profiles ${SpringConfigStoreByName.getOrCreate(projectName).profilesActive} nor in profile 'default' config values")
}
fun getSpringConfig(projectName: String, compoundKey: String): String {
    return getSpringConfig(projectName, compoundKey.split('.'))
}

private fun getValueOfCompoundKeyRecursion(projectName: String, index: Int, sofar: MutableList<String>, keyList: List<String>, valueMapOfMaps: Map<String, Any> ): List<String> {
//println "index: ${index} sofar: '" + sofar.join('.') + "' compoundKey: '" + compoundKey.join('.') + "' map: '" + valueMapOfMaps.keySet() + "'"
    val currentKeyPart = keyList.get(index)
    sofar.add(currentKeyPart)
    if (valueMapOfMaps.containsKey(currentKeyPart)) {
        val o = valueMapOfMaps.get(currentKeyPart)
        if (o is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return getValueOfCompoundKeyRecursion(projectName, index+1, sofar, keyList, o as Map<String, Any>)
        } else if(o is List<*>) {
            return listOf(o.joinToString(","))
        } else if (o is String) {
            var s = o.toString()
            if (s.startsWith("\${") && s.endsWith("}")) {
                s = s.substring(2);
                s = s.substring(0, s.length - 1);
                val indexOfColon = s.indexOf(":");
                var variable = if(indexOfColon >= 0) s.substring(0, indexOfColon) else s
                var defaultValue = if(indexOfColon >= 0) s.substring(indexOfColon + 1) else "XXXYYYXXXZZZ";
                var value = System.getenv(variable)
                if (value != null) { return listOf(value) }
                value = System.getenv(variable.toUpperCase())
                if (value != null) { return listOf(value) }
                value = System.getenv(variable.replace('.', '_'))
                if (value != null) { return listOf(value) }
                value = System.getenv(variable.replace('.', '_').toUpperCase())
                if (value != null) { return listOf(value) }
                try {
                    value = getSpringConfig(projectName, variable)
                    return listOf(value)
                } catch(ex: GradleException) {
                    if(defaultValue == "XXXYYYXXXZZZ") {
                        throw GradleException("${keyList.joinToString(".")}: \"\${${s}}\" variable definition neither found in any active spring profile nor in spring default properties nor in any system environment variable")
                    }
                    return listOf(defaultValue)
                }
            } else if(s.startsWith("systemProp.")) {
                return listOf(secure.getPropertyFromUserHomeDotGradleGradleProperties(s.substring("systemProp.".length)))
            } else {
                return listOf(s)
            }
        } else {
            return listOf(o.toString())
        }
    }
    return listOf()
}

fun loadSpringAppConfigs(projectName: String, vararg yamlFiles: String) {
    println("loadSpringAppConfig for project: ${projectName}")
    if (System.getenv("SPRING_PROFILES_ACTIVE") != null) {
        SpringConfigStoreByName.getOrCreate(projectName).profilesActive.addAll(System.getenv("SPRING_PROFILES_ACTIVE").split(','))
    } else {
        SpringConfigStoreByName.getOrCreate(projectName).profilesActive.add("default")
    }
    val parser = Yaml()
    var counter = 0
    for (configFile in yamlFiles) {
        if (File(configFile).exists()) {
            SpringConfigStoreByName.getOrCreate(projectName).configFilesDefined.add(configFile)
        } else {
            continue
        }
        for (data in parser.loadAll(File(configFile).inputStream())) {
            var profileNames = linkedSetOf("default")
            @Suppress("UNCHECKED_CAST")
            var map = data as Map<String, Any>
            if (map.containsKey("spring")) {
                @Suppress("UNCHECKED_CAST")
                var mapSpring = map["spring"] as Map<String, Any>
                if ( mapSpring.containsKey("profiles") ) {
                    profileNames = linkedSetOf()
                    val springProfiles = mapSpring["profiles"]
                    if(springProfiles is String) {
                        profileNames.addAll(springProfiles.split(','))
                    } else if(springProfiles is List<*>) {
                        for(profile in springProfiles) {
                            profileNames.add(profile as String)
                        }
                    }
                }
                if (SpringConfigStoreByName.getOrCreate(projectName).profilesActive.intersect(profileNames).size > 0) {
                    val springProfilesInclude = mapSpring["profiles.include"]
                    if(springProfilesInclude != null) {
                        if(springProfilesInclude is String) {
                            SpringConfigStoreByName.getOrCreate(projectName).profilesActive.addAll(springProfilesInclude.toString().split(','))
                        } else if(springProfilesInclude is List<*>) {
                            for(toInclude in springProfilesInclude) {
                                SpringConfigStoreByName.getOrCreate(projectName).profilesActive.add(toInclude as String)
                            }
                        }
                    }
                }
            }
            // remember profile names
            SpringConfigStoreByName.getOrCreate(projectName).profileNames.addAll(profileNames)
            SpringConfigStoreByName.getOrCreate(projectName).otherProfileNames.addAll(profileNames.filter{it != "default"})
            // add all profile specific key/values to map under its profile name into springConfigValuesByProfile
            val normalizedMap = normalizeKeys(map)
            for(profileName in profileNames) {
                var projectAllProfilesMap = SpringConfigStoreByName.getOrCreate(projectName).springConfigValuesByProfile
                projectAllProfilesMap[profileName] = deepMergeMaps(0, normalizedMap, projectAllProfilesMap[profileName], LinkedList<String>(), "project '${projectName}'s profile '${profileName}' in '${configFile}' ")
            }
            counter++;
        }
    }
    if ( counter == 1) {
        println("loadSpringAppconfigs() for project '${projectName}' parsed ${SpringConfigStoreByName.getOrCreate(projectName).configFilesDefined} \nfound only definitions for profile: default")
    } else {
        println("loadSpringAppconfigs() for project '${projectName}' parsed ${SpringConfigStoreByName.getOrCreate(projectName).configFilesDefined} \nfound ${counter-1} yaml documents overall in them which define profiles: " + SpringConfigStoreByName.getOrCreate(projectName).otherProfileNames.joinToString(","))
    }
    println("SPRING_PROFILES_ACTIVE: ${SpringConfigStoreByName.getOrCreate(projectName).profilesActive}")
}

private fun deepMergeMaps(index: Int, toMergeMap1: Map<String, Any>, toMergeMap2: Map<String, Any>?, keyPartsList: LinkedList<String>, description: String): Map<String, Any> {
    val resultMap = LinkedHashMap<String, Any>()
    resultMap.putAll(toMergeMap1)

    if (toMergeMap2 == null) {
        return resultMap
    }

    var exceptionTexts = ArrayList<String>()
    for ((keyFrom2, valueFrom2) in toMergeMap2) {
        val value1 = resultMap[keyFrom2]
        val value2 = valueFrom2
        if ( (value1 is Map<*, *>) && (value2 is Map<*, *>) ) {
            keyPartsList.add(keyFrom2)
            @Suppress("UNCHECKED_CAST")
            val innerMap = deepMergeMaps(index+1, value1 as Map<String, Any>, value2 as Map<String, Any>, keyPartsList, description)
            keyPartsList.removeLast()
            resultMap.put(keyFrom2, innerMap)
        } else {
            val currentKey = "${keyPartsList.joinToString(".")}.${keyFrom2}"
            if("spring.profiles" != currentKey) {
                if (resultMap.containsKey(keyFrom2) && (value1 != value2)) {
                    exceptionTexts.add("${description} contains multiple times key '${currentKey}' with different values '${value1}' != '${value2}'")
                }
            }
            resultMap.put(keyFrom2, value2)
        }
    }
    if(exceptionTexts.isNotEmpty()) {
        var exTexts = ""
        for(text in exceptionTexts) {
            println("Exception: " + text)
            exTexts += "\n${text}\n"
        }
        throw GradleException(exTexts)
    }

    return resultMap
}

private fun normalizeKeys(map: Map<String, Any>): Map<String, Any> {
    val resultMap = LinkedHashMap<String, Any>()
    for((k, v) in map) {
        val keys = k.split(".")
        if((keys.size > 1) && (k != "profiles.include")) {
            var newMap = LinkedHashMap<String, Any>()
            resultMap.put(keys.elementAt(0), newMap)
            for(i in 1 until keys.size - 1) { // intermediate keys
                val intermediateMap = LinkedHashMap<String, Any>()
                newMap.put(keys.elementAt(i), intermediateMap)
                newMap = intermediateMap
            }
            if(v is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                newMap.put(keys.elementAt(keys.size - 1), normalizeKeys(v as Map<String, Any>))
            } else {
                newMap.put(keys.elementAt(keys.size - 1), v)
            }
        } else {
            if(v is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                resultMap.put(k, normalizeKeys(v as Map<String, Any>))
            } else {
                resultMap.put(k, v)
            }
        }
    }
    return resultMap
}

fun dumpSpringConfigValues(projectName: String, profile: String) {
    dumpMap(0, SpringConfigStoreByName.getMapByProfile(projectName, profile))
}
fun dumpSpringConfigValues(projectName: String) {
    for((k, v) in SpringConfigStoreByName.getOrCreate(projectName).springConfigValuesByProfile) {
        println("===================================================")
        println("spring.profile = ${k}")
        println("===================================================")
        dumpMap(0, v)
    }
}

private fun dumpMap(level: Int, map: Map<String, Any>) {
    var indent = ""
    for (x in 0 until (level * 2)) {
        indent += " "
    }
    for((k, v) in map) {
        if(v is Map<*, *>) {
            println(indent + k + ":")
            @Suppress("UNCHECKED_CAST")
            dumpMap(level+1, v as Map<String, Any>)
        } else if(v is List<*>) {
            println(indent + k + ":")
            for(l in v) {
                if(v is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    dumpMap(level+1, v as Map<String, Any>)
                } else {
                    println(indent + "- " + l)
                }
            }
        } else {
            println(indent + k + ": " + v)
        }
    }
}