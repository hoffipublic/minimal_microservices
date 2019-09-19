#!/bin/bash

err=false
if [ -z "$1" ] || [ -z "$2" ] || ! [[ $1 =~ ^(bind|unbind|nobind)$ ]]; then
  echo "synopsis: $0 <bind|nobind> <serviceInstancesNamePrefix> <optional:pathToMicroserviceClient.jar>"
  err=true
else
  bind=$1
  prefix=$2
fi
if [ "${PWD##*/}" = 'scripts' ]; then
  echo 'please call script from (multiproject) root dir'
  err=true
fi
if [ ! -z "$1" ] || [ ! -z "$2" ] && { [ ! -e "$3" ] || [ ! -r "$3" ]; }; then
  echo "  microservice jar not found under: $3 ... defaulting to ./microservice/build/libs/minimal_microservice-0.1.0.RELEASE.jar"
  jarPath="./microservice/build/libs/minimal_microservice-0.1.0.RELEASE.jar"
fi
if [ "$err" = "true" ]; then
  exit 255
fi

sysDomain=$(command cf api | grep 'api endpoint' | sed -E 's/^api endpoint: +https?:\/\/api.(.+)$/\1/')
appsDomain=apps.${sysDomain#sys.}
echo " sys-domain:  $sysDomain"
echo "apps-domain: $appsDomain"

cf service ${prefix}ConfigServer 2>&1 >/dev/null
if [ $? -eq 0 ]; then
  withConfigServer=true
else
  withConfigServer=false
fi


set -e

zipkinservername=${prefix}ZipkinServer

if [ -e "./zipkinserver/zipkin.jar" ]; then
  set -x
  cf push ${zipkinservername} -p ./zipkinserver/zipkin.jar -b java_online &
  set +x
fi

set -x

additionalSpringProfiles=",sleuth"

for profile in sink tier2 tier1 source ; do
  appName="microservice_$profile"
  echo "pushing ${appName}_$prefix"

  cf push ${appName}_$prefix -k 512M -m 1G -i 1 -p "$jarPath" --no-start -b java_online

  cf set-env ${appName}_$prefix JBP_CONFIG_OPEN_JDK_JRE '{ jre: { version: 9.+ } }'
  cf set-env ${appName}_$prefix SPRING_APPLICATION_NAME "$appName"
  cf set-env ${appName}_$prefix ZIPKIN_URL "http://${zipkinservername}.$appsDomain"
  cf set-env ${appName}_$prefix SPRING_PROFILES_ACTIVE "$profile$additionalSpringProfiles"

  if [ "$bind" = "bind" ]; then
    cf bind-service ${appName}_$prefix ${prefix}ServiceRegistry
    cf bind-service ${appName}_$prefix ${prefix}CircuitBreaker
    cf bind-service ${appName}_$prefix ${prefix}RabbitShared
    if [ "$withConfigServer" == "true" ]; then
	    cf bind-service ${appName}_$prefix ${prefix}ConfigServer
	fi
    cf start ${appName}_$prefix
  else
    cf restart ${appName}_$prefix
  fi
done 

set +x
