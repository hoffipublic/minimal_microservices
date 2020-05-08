#!/usr/bin/env bash

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SUBPROJECTROOTDIR=${SCRIPTDIR%/*}

finish() {
    errorcode=$?
    set +x
    return $errorcode
}
trap finish EXIT

CMD=apply
if [[ -n "$1" ]];then
    if [[ "$1" = "replace" ]]; then
        CMD="replace --force"
    elif [[ "$1" = "delete" ]]; then
        CMD="delete"
    else
        2> echo "cmd '$1' unknown"
        exit
    fi
fi

set -ue
set -x

for s in $(echo "sink" "tier2" "tier1" "source"); do
    sed "s/default/$s/g" ${SUBPROJECTROOTDIR}/generated/microservice-deployment.yml | kubectl $CMD -f -
    sleep 4
done
