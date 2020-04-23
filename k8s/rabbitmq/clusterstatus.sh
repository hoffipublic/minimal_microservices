#!/usr/bin/env bash

FIRST_POD=$(kubectl get pods --namespace rabbitmq-ns -l 'app=rabbitmq' -o jsonpath='{.items[0].metadata.name }')
kubectl exec --namespace=rabbitmq-ns $FIRST_POD -- rabbitmq-diagnostics cluster_status