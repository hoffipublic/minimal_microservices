#!/bin/bash

# you have to be logged in to concourse on running this script
# e.g. by
# eval $(~/bosh/bucc/bin/bucc int)
# export PATH=$PATH:~/bosh/bucc/bin
# bucc fly

set -e
SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
set -x
source $SCRIPTDIR/00scriptVariables.sh

fly -t bucc set-pipeline --non-interactive \
    -p $env-$pipelineName -c $SCRIPTDIR/../pipeline.yml \
    --load-vars-from=$SCRIPTDIR/../variables.yml
set +x