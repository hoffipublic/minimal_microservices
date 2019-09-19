#!/bin/bash

fixedDelay=2500
if [ -z "$1" ]; then
	echo "resetting fixedDelay to $fixedDelay"
else
    fixedDelay=$1
    echo "setting fixedDelay to $fixedDelay"
fi

set -x
curl -X POST http://localhost:8083/actuator/env -d "app.sources.fixedDelay=$fixedDelay"
echo ""
curl -X POST http://localhost:8083/actuator/refresh
echo ""
set +x
