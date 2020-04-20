package spring

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.LinkedList
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.Map
import org.gradle.api.GradleException

enum class ConfType(val logText: String) {
    APP("spring appConfig"),
    ENV("environment config")
}

// singleton definitions
object SpringConfigStore {
    private val springAppConfigsByProjectName = LinkedHashMap<String, ProjectConfig>()
    private val envConfigsByProjectName = LinkedHashMap<String, ProjectConfig>()

    class ProjectConfig {
        val configFilesDefined = LinkedHashSet<String>()
        val profilesActive = LinkedHashSet<String>()
        val profileNames = LinkedHashSet<String>()
        val otherProfileNames = LinkedHashSet<String>()
        val configValuesByProfile = LinkedHashMap<String, Map<String, Any>>()
    }

    fun getOrCreate(confType: ConfType, projectName: String): ProjectConfig {
        var config :ProjectConfig? = null
        when (confType) {
            ConfType.APP -> {
                config = springAppConfigsByProjectName[projectName]
                if (config == null) {
                    config = ProjectConfig()
                    springAppConfigsByProjectName[projectName] = config
                }
            }
            ConfType.ENV -> {
                config = envConfigsByProjectName[projectName]
                if (config == null) {
                    config = ProjectConfig()
                    envConfigsByProjectName[projectName] = config
                }
            }
        }
        return config
    }

    fun getSpringActiveProfiles(projectName: String): LinkedHashSet<String> {
        var config :ProjectConfig? = springAppConfigsByProjectName[projectName]
        if (config == null) {
            throw GradleException("no ${ConfType.APP.logText}s read by gradle (sub)project ${projectName}")
        }
        return config.profilesActive
    }


    fun getMapByProfile(confType: ConfType, projectName: String, profile: String): Map<String, Any> {
        var config :ProjectConfig?
        var map :Map<String, Any>? = null
        when (confType) {
            ConfType.APP -> {
                config = springAppConfigsByProjectName[projectName]
                if (config == null) {
                    throw GradleException("no ${confType.logText}s read by gradle (sub)project ${projectName}")
                }
                map = config.configValuesByProfile[profile]
                if (map == null) {
                    //throw GradleException("project: '${projectName}' has no ${confType.logText} values for profile '${profile}' defined!")
                    return emptyMap()
                }
            }
            ConfType.ENV -> {
                config = envConfigsByProjectName[projectName]
                if (config == null) {
                    throw GradleException("no ${confType.logText}s read by gradle (sub)project ${projectName}")
                }
                map = config.configValuesByProfile[profile]
                if (map == null) {
                    throw GradleException("project: '${projectName}' has no ${confType.logText} values for profile '${profile}' defined!")
                }
            }
        }
        return map
    }
}

fun getSpringActiveProfiles(projectName: String): String {
    return SpringConfigStore.getSpringActiveProfiles(projectName).joinToString(",")
}

fun getSpringAppConfig(projectName: String, compoundKey: String): String {
    return getConfig(ConfType.APP, projectName, compoundKey.split('.'))
}
fun getSpringAppConfig(projectName: String, keyList: List<String>): String {
    return getConfig(ConfType.APP, projectName, keyList)
}

fun getEnvConfig(projectName: String, compoundKey: String): String {
    return getConfig(ConfType.ENV, projectName, compoundKey.split('.'))
}
fun getEnvConfig(projectName: String, keyList: List<String>): String {
    return getConfig(ConfType.ENV, projectName, keyList)
}

private fun getConfig(confType: ConfType, projectName: String, keyList: List<String>): String {
    val values: MutableSet<String> = mutableSetOf()
    for(activeProfile in SpringConfigStore.getOrCreate(confType, projectName).profilesActive) {
        val sofar: MutableList<String> = mutableListOf()
        values.addAll(getValueOfCompoundKeyRecursion(confType, projectName, 0, sofar, keyList, SpringConfigStore.getMapByProfile(confType, projectName, activeProfile)))
    }
    if(values.size == 0) {
        val sofar: MutableList<String> = mutableListOf()
        values.addAll(getValueOfCompoundKeyRecursion(confType, projectName, 0, sofar, keyList, SpringConfigStore.getMapByProfile(confType, projectName, "default")))
    }
    if(values.size == 1) {
        return values.elementAt(0)
    }
    if(values.size > 1) {
        throw GradleException("project: '${projectName}' has ambiguous values for ${confType.logText} ${keyList.joinToString(".")}:'${values.joinToString("','")}' in active profiles (or environment)  ${SpringConfigStore.getOrCreate(confType, projectName).profilesActive}")
    }
    throw GradleException("no values found for ${confType.logText} '${keyList.joinToString(".")}' neither in active profiles (or environment) ${SpringConfigStore.getOrCreate(confType, projectName).profilesActive} nor in profile/environment 'default' config values")
}

private fun getValueOfCompoundKeyRecursion(confType: ConfType, projectName: String, index: Int, sofar: MutableList<String>, keyList: List<String>, valueMapOfMaps: Map<String, Any> ): List<String> {
//println "index: ${index} sofar: '" + sofar.join('.') + "' compoundKey: '" + compoundKey.join('.') + "' map: '" + valueMapOfMaps.keySet() + "'"
    val currentKeyPart = keyList.get(index)
    sofar.add(currentKeyPart)
    if (valueMapOfMaps.containsKey(currentKeyPart)) {
        val o = valueMapOfMaps.get(currentKeyPart)
        if (o is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return getValueOfCompoundKeyRecursion(confType, projectName, index+1, sofar, keyList, o as Map<String, Any>)
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
                    when(confType) {
                        ConfType.APP -> { value = getSpringAppConfig(projectName, variable) }
                        ConfType.ENV -> { value = getEnvConfig(projectName, variable) }
                    }
                    return listOf(value)
                } catch(ex: GradleException) {
                    if(defaultValue == "XXXYYYXXXZZZ") {
                        throw GradleException("${keyList.joinToString(".")}: \"\${${s}}\" variable definition for ${confType.logText} neither found in any active  profile nor in spring default properties nor in any system environment variable")
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
    loadConfigs("XXXDUMMYXXX", ConfType.APP, projectName, *yamlFiles)
}
fun loadEnvConfigs(env: String, projectName: String, vararg yamlFiles: String) {
    loadConfigs(env, ConfType.ENV, projectName, *yamlFiles)
}

private fun loadConfigs(env: String, confType: ConfType, projectName: String, vararg yamlFiles: String) {
    if ( (confType == ConfType.APP) && System.getenv("SPRING_PROFILES_ACTIVE") != null) {
        SpringConfigStore.getOrCreate(confType, projectName).profilesActive.addAll(System.getenv("SPRING_PROFILES_ACTIVE").split(','))
    } else if( (confType == ConfType.ENV) && (env != "default") ) {
        SpringConfigStore.getOrCreate(confType, projectName).profilesActive.clear()
        SpringConfigStore.getOrCreate(confType, projectName).profilesActive.addAll(env.split(','))
    } else {
        SpringConfigStore.getOrCreate(confType, projectName).profilesActive.add("default")
    }
    val parser = Yaml()
    var counter = 0
    for (configFile in yamlFiles) {
        if (File(configFile).exists()) {
            SpringConfigStore.getOrCreate(confType, projectName).configFilesDefined.add(configFile)
        } else {
            continue
        }
        for (data in parser.loadAll(File(configFile).inputStream())) {
            var profileNames = mutableListOf("default")
            @Suppress("UNCHECKED_CAST")
            var map = data as Map<String, Any>
            if (map.containsKey("spring")) {
                @Suppress("UNCHECKED_CAST")
                var mapSpring = map["spring"] as Map<String, Any>
                if ( mapSpring.containsKey("profiles") ) {
                    val springProfiles = mapSpring["profiles"]
                    if(springProfiles is String) {
                        profileNames = mutableListOf()
                        profileNames.addAll(springProfiles.split(','))
                    } else if(springProfiles is List<*>) {
                        profileNames = mutableListOf()
                        for(profile in springProfiles) {
                            profileNames.add(profile as String)
                        }
                    }
                }
                if ( (profileNames[0] == "default") || (SpringConfigStore.getOrCreate(confType, projectName).profilesActive.intersect(profileNames).isNotEmpty()) ) {
                    val springProfilesInclude = mapSpring["profiles.include"]
                    if(springProfilesInclude != null) {
                        if(springProfilesInclude is String) {
                            SpringConfigStore.getOrCreate(confType, projectName).profilesActive.addAll(springProfilesInclude.toString().split(','))
                        } else if(springProfilesInclude is List<*>) {
                            for(toInclude in springProfilesInclude) {
                                SpringConfigStore.getOrCreate(confType, projectName).profilesActive.add(toInclude as String)
                            }
                        }
                    }
                } // else inactive profile(s)
            }
            // remember profile names
            SpringConfigStore.getOrCreate(confType, projectName).profileNames.addAll(profileNames)
            SpringConfigStore.getOrCreate(confType, projectName).otherProfileNames.addAll(profileNames.filter{it != "default"})
            // add all profile specific key/values to map under its profile name into springConfigValuesByProfile
            val normalizedMap = normalizeKeys(map)
            for(profileName in profileNames) {
                var projectAllProfilesMap = SpringConfigStore.getOrCreate(confType, projectName).configValuesByProfile
                projectAllProfilesMap[profileName] = deepMergeMaps(0, normalizedMap, projectAllProfilesMap[profileName], LinkedList<String>(), "project '${projectName}'s profile '${profileName}' in '${configFile}' ")
            }
            counter++;
        }
    }
    if ( counter == 2) {
        println("load ${confType.logText} for project '${projectName}' parsed ${SpringConfigStore.getOrCreate(confType, projectName).configFilesDefined} \nfound only definitions for profile: default")
    } else {
        println("load ${confType.logText} for project '${projectName}' parsed ${SpringConfigStore.getOrCreate(confType, projectName).configFilesDefined} \nfound ${counter-1} yaml documents overall which define profiles: " + SpringConfigStore.getOrCreate(confType, projectName).otherProfileNames.joinToString(","))
    }
    if(confType == ConfType.APP) {
        println("SPRING_PROFILES_ACTIVE: ${SpringConfigStore.getOrCreate(confType, projectName).profilesActive}")
    } else {
        println("-Penv=env(s) active:    ${SpringConfigStore.getOrCreate(confType, projectName).profilesActive}")
    }
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

fun dumpSpringConfigValues(confType: ConfType, projectName: String, profile: String) {
    dumpMap(0, SpringConfigStore.getMapByProfile(confType, projectName, profile))
}
fun dumpSpringConfigValues(confType: ConfType, projectName: String) {
    for((k, v) in SpringConfigStore.getOrCreate(confType, projectName).configValuesByProfile) {
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