plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation("org.yaml:snakeyaml:1.25") // https://bitbucket.org/asomov/snakeyaml/wiki/Documentation
}
