package secure

fun getPropertyFromUserHomeDotGradleGradleProperties(key: String): String {
    if (System.getenv("JOB_NAME") != null) {
        println("Building on build-server, if needed, ${key} has to be provided by the build-server")
        return "provided by CICD system"
    }
    var value: String? = System.getProperty(key)
    if(value == null) {
        val pathToGradleProp = System.getProperty("user.home") + "/.gradle/gradle.properties"
        System.err.println("\n\n=======================================================================================")
        System.err.println("If building locally, please add the following security sensitive property to your local '${pathToGradleProp}' by adding the following line:\n")
        System.err.println("\tsystemProp.${key}=yourSecureValueHere")
        System.err.println("===========================================================================================\n")
        // System.exit(1);
        return "<missing-key 'systemProp.${key}' in ~/.gradle/gradle.properties>"
    } else {
        return value.toString()
    }
}
