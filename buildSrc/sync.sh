#!/usr/bin/env bash

MYDIFFDIR=$HOME/DBs/git/minimal/gradle_dependencies/buildSrc
SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )" # dir where this script is in
BUILDSRC=${SCRIPTDIR##*/} # last dir, ergo "buildSrc"

for f in $(find ${MYDIFFDIR} -type d \( -name build -o -name .git -o -name .gradle \) -prune -o -type f \( -name .gitignore -o -name .DS_Store -o -name Readme.md \) -prune -o -type f -print); do
    current=${f#${MYDIFFDIR}/} # strip leading MYDIFFDIR from f
    echo
    echo ${BUILDSRC}/${current}
    echo ${current//?/=} # underline
    if [[ -f ${SCRIPTDIR}/${current} ]]; then
        if cmp --silent ${SCRIPTDIR}/${current} ${MYDIFFDIR}/${current}; then
            echo -e "${BUILDSRC}/${current} is ${colGreen}equal${colReset}"
        else
            echo -e "opendiff ${BUILDSRC}/${current}  gradle_dependencies/buildSrc/$current -merge ${colRed}${BUILDSRC}/${current}${colReset}"
            /usr/bin/opendiff ${SCRIPTDIR}/${current} ${MYDIFFDIR}/${current} -merge ${SCRIPTDIR}/${current}
        fi
    else
        echo -e "${colOrange}${BUILDSRC}/${current}${colReset} is a dir or does not exist here"
    fi
done

