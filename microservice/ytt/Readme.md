gradle task `generateK8s` defined in `./buildfiles/buildK8s.gradle.kts`

`./ytt/envVars/defaults.yml` is always used by ytt

`./ytt/envVars/xy.yml` is read depending on `-Penv=<xy>` gradle property (defaults to 'default')

`generateK8s` task will generate further ytt data files to `./ytt/generated/` dir

rendered templates go to `./generated/` dir