#!/bin/bash

err=false
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "synopsis: $0 <https://api.<sysDomain> <serviceInstancesNamePrefix> <pathToConfigServerJsonConfigFile>"
  err=true
else
  prefix=$2
fi
if [ "${PWD##*/}" = 'scripts' ]; then
  echo 'please call script from (multiproject) root dir'
  err=true
fi
if [ ! -z "$3" ] && { [ ! -e "$3" ] || [ ! -r "$3" ] ; }; then
  echo "  no readable configServer json file found under: $2"
  err=true
else
  configServerJsonFile=$3
fi
if [ "$err" = "true" ]; then
  exit -1
fi

cf login -a $1 --sso

cf create-service p-rabbitmq                  standard  ${prefix}RabbitShared &
cf create-service p-service-registry          standard  ${prefix}ServiceRegistry &
cf create-service p-circuit-breaker-dashboard standard  ${prefix}CircuitBreaker &
if [ ! -z "$configServerJsonFile" ]; then
  cf create-service p-config-server             standard  ${prefix}ConfigServer -c $configServerJsonFile &
fi

echo 'watch cf services'

