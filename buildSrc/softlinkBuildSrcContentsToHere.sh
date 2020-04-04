#!/usr/bin/env bash

centralGradleDependenciesDir='../../gradle_dependencies/buildSrc'

function cleanup {
  set +x
}
trap cleanup EXIT

mkdir -p src/main/kotlin/c
mkdir -p src/main/kotlin/secure
mkdir -p src/main/kotlin/spring
mkdir -p src/main/kotlin/v

function backDirs() { local result=".." ; for ((i=1; i<$(echo "${1%/}" | awk -F"/" '{print NF-1}'); i+=1)); do result+="/.."; done ; echo $result ; }

files=( src/main/kotlin/c/commons.kt src/main/kotlin/secure/security.kt src/main/kotlin/spring/springProperties.kt src/main/kotlin/v/dependencies.kt )

# latest versions in https://github.com/hoffipublic/gradle_dependencies
>&2 echo ln -s $centralGradleDependenciesDir/build.gradle.kts
ln -s $centralGradleDependenciesDir/build.gradle.kts
for f in ${files[@]}; do
    >&2 echo ln -s $(backDirs $f)/$centralGradleDependenciesDir/$f ./$f
    ln -s $(backDirs $f)/$centralGradleDependenciesDir/$f ./$f
done
