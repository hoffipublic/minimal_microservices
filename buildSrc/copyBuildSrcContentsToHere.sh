#!/usr/bin/env bash

# latest versions in https://github.com/hoffipublic/gradle_dependencies

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

set -u # exit if a variable(value) is not defined

pushd "$SCRIPTDIR"
relativeGradleDependenciesDir=../../gradle_dependencies
centralGradleDependenciesDir="${SCRIPTDIR}/${relativeGradleDependenciesDir}"
buildSrcDir=${centralGradleDependenciesDir}/buildSrc
srcDir=${buildSrcDir}/src

function cleanup {
  set +x
  popd
}
trap cleanup EXIT


# mkdirs of buildSrc/src subdirs
find ${srcDir} -type d -print0 | while IFS= read -r -d '' p; do
    # remove longest prefix of path (which is exactly ${buildSrcDir})
    shortname=${p##*$(basename ${buildSrcDir})/}
    if [[ -d "${shortname}" ]]; then continue ; fi
    >&2 echo mkdir -p \"${shortname}\"
             mkdir -p   ${shortname}
done

# copy src files
filesNotToCopy=( .DS_Store ownVersion.kt )
find ${srcDir} -type f -print0 | while IFS= read -r -d '' p; do
    # remove longest prefix of path (which is exactly ${buildSrcDir})
    srcShortname=${p##*${SCRIPTDIR}/}
    destShortname=${p##*$(basename ${buildSrcDir})/}
    for skipname in ${filesNotToCopy[@]}; do if [[ $(basename "$p") = "$skipname" ]]; then >&2 echo "skipping $(basename "$p") (${srcShortname})" ; continue 2 ; fi ; done
    >&2 echo rm \"${destShortname}\"
             rm  "${destShortname}" 2> /dev/null
    >&2 echo cp \"${srcShortname}\" \"${destShortname}\"
             cp  "${srcShortname}"   "${destShortname}"
done

# copy buildSrc project files
filesToCopy=( build.gradle.kts )
for f in ${filesToCopy[@]}; do
    srcShortname=${relativeGradleDependenciesDir}/buildSrc/$f
    >&2 echo rm \"${f}\"
             rm  "${f}" 2> /dev/null
    >&2 echo cp \"${srcShortname}\" \"${f}\"
             cp  "${srcShortname}"   "${f}"
done

# copy root-project files
filesToCopy=( environments.yml )
for f in ${filesToCopy[@]}; do
    srcShortname=${relativeGradleDependenciesDir}/$f
    destShortname="../$f"
    >&2 echo rm \"${destShortname}\"
             rm  "${destShortname}" 2> /dev/null
    >&2 echo cp \"${srcShortname}\" \"${destShortname}\"
             cp  "${srcShortname}"   "${destShortname}"
done
