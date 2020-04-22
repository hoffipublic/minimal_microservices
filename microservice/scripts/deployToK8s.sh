#!/usr/bin/env bash

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

for s in $(echo "sink" "tier2" "tier1" "source"); do
    sed "s/default/$s/g" generated/microservice-deployment.yml | kubectl $CMD -f -
    sleep 4
done
