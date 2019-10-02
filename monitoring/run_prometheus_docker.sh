#!/bin/bash

ipLocal=$(ifconfig | sed -En 's/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p' | grep --color=always -v -E '^(127)')
appName=minimal_microservice
appPort=8080

echo "execute on console terminal:"
echo "============================"

cat <<EOTEXT

docker run -d -p 9090:9090 --name prometheus prom/prometheus

docker exec -it prometheus /bin/sh

cat <<EOT >>  /etc/prometheus/prometheus.yml
  - job_name: '${appName}'
    metrics_path: /actuator/prometheus
    scrape_interval: 5s
    static_configs:
    - targets: ['${ipLocal}:${appPort}']
EOT

or specifically for all microservice instances

cat <<EOT >> /etc/prometheus/prometheus.yml
  - job_name: 'microservice_source'
    metrics_path: /actuator/prometheus
    scrape_interval: 5s
    static_configs:
    - targets: ['192.168.0.2:8083']
  - job_name: 'microservice_tier1'
    metrics_path: /actuator/prometheus
    scrape_interval: 5s
    static_configs:
    - targets: ['192.168.0.2:8082']
  - job_name: 'microservice_tier2'
    metrics_path: /actuator/prometheus
    scrape_interval: 5s
    static_configs:
    - targets: ['192.168.0.2:8081']
  - job_name: 'microservice_sink'
    metrics_path: /actuator/prometheus
    scrape_interval: 5s
    static_configs:
    - targets: ['192.168.0.2:8080']
EOT

exit

docker restart prometheus

open http://localhost:9090/targets

EOTEXT

echo "and then configure Grafana data source like:"
echo "============================================"

cat <<EOTEXT

docker run -d -p 3000:3000 --name grafana grafana/grafana

docker exec -ti grafana grafana-cli admin reset-admin-password 'admin'

exit

Grafana    URL: http://localhost:3000

define data-source yourself:
- data source: prometheues
- URL http://${ipLocal}:9090


prometheus URL: http://${ipLocal}:9090
Grafana    URL: http://localhost:3000

+ import
cat grafana/minimal_spring_prometheus.json | pbcopy


EOTEXT
