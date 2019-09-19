#!/bin/bash

justEcho=false
if [[ "$1" == dry* ]] || [[ "$2" == dry* ]]; then # just echo don't start
  justEcho=true
  shift
fi
additionalProfiles="" # comma separated without spaces
if [ ! -z "$1" ]; then
  additionalProfiles=",$1"
fi

# call this script from root of minimal_microservices:microservice subproject
declare -a theCmds=()
declare -i port=8079
for tier in sink tier2 tier1 source ; do
  ((port+=1))
  theCmd="SERVER_PORT=$port SPRING_PROFILES_ACTIVE=local,$tier$additionalProfiles build/libs/minimal_microservices-0.1.0.RELEASE.jar --spring.application.name=microservice_$tier --app.businessLogic.tier=$tier --logging.file=microservice_$tier.log"
  echo $theCmd
  if [ ! "$justEcho" = true ]; then
    theCmds+=("$theCmd")
  fi
done

if [ ! "$justEcho" = true ]; then
  echo -----------------------------------
  echo executing...
fi

for ((i = 0; i < ${#theCmds[@]}; i++)); do
  echo "${theCmds[$i]}"
  eval ${theCmds[$i]} &
  sleep 5000
done
