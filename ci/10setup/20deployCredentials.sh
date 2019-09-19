#!/bin/bash

# you have to be logged in to credhub on running this script
# e.g. by
# eval $(~/bosh/bucc/bin/bucc int)
# export PATH=$PATH:~/bosh/bucc/bin
# bucc credhub

set -e
SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source $SCRIPTDIR/00scriptVariables.sh

if [[ $# -ne 3 ]]; then
  USAGE=$(cat <<-EOT
usage: $0 <param1> ... <paramX>
parameter order:
  cfPushTuUsername \
  cfPushTuPassword \
  gitPrivateRepoKeyFilePath \
  configServerGitRepoKeyOneLiner
EOT
)
fi

cfPushTuUsername=$1
cfPushTuPassword=$2
gitPrivateRepoKeyFilePath=$3
configServerGitRepoKeyOneLiner=$4

if [ -z "$cfPushTuUsername" ]; then
  read -p "cfPushTuUsername (admin): " cfPushTuUsername
  cfPushTuUsername=${cfPushTuUsername:-admin}
fi
set -x
credhub set -n /concourse/$team/$env-$pipelineName/cf-tu-username \
  -t value -v "$cfPushTuUsername"
set +x

if [ -z "$cfPushTuPassword" ]; then
  read -p "cfPushTuPassword: " cfPushTuPassword
fi
set -x
credhub set -n /concourse/$team/$env-$pipelineName/cf-tu-password \
  -t value -v "$cfPushTuPassword"
set +x

if [ -z "$gitPrivateRepoKeyFilePath" ]; then
  read -p "gitPrivateRepoKeyFilePath: " gitPrivateRepoKeyFilePath
fi
set -x
credhub set -n /concourse/$team/$env-$pipelineName/git-private-repo-key \
  -t rsa --private "$gitPrivateRepoKeyFilePath"
set +x

if [ -z "$configServerGitRepoKeyOneLiner" ]; then
  echo 'line breaks have to be replaced by "\\n" (because this shell-script also expands)!!!'
  read -p "configServerGitRepoKeyOneLiner: " configServerGitRepoKeyOneLiner
fi
set -x
credhub set -n /concourse/$team/$env-$pipelineName/configServerGitRepoKeyOneLiner \
  -t value -v "$configServerGitRepoKeyOneLiner"
set +x
