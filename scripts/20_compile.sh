#!/bin/bash

err=false
if [ -z "$1" ]; then
  echo "synopsis: $0 <serviceInstancesNamePrefix>"
  err=true
else
  prefix=$1
fi
if [ "${PWD##*/}" = 'scripts' ]; then
  echo 'please call script from (multiproject) root dir'
  err=true
fi
if [ "$err" = "true" ]; then
  exit -1
fi

set -x
find . -type f -a \( -name "*.yml" -o -name "*.properties" -o -name "*.gradle" \) -exec sed -i "" "s/testpo/$prefix/g" {} +
set +x

./gradlew clean build -x test

