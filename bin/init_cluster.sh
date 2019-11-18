#!/usr/bin/env bash

# init_cluster.sh - bootstraps a K8s cluster.
#
# Installs:
# - Helm / Tiller
# - Secret containing a service-account key for interacting with CloudDNS
# - cert-manager for provisioning TLS certificates using Let's Encrypt
#
# Usage: init_cluster.sh ENV
#
# ENV: one of 'staging' or 'production'
#

set -eo pipefail

CERT_MANAGER_VERSION=0.11

if [[ -z "$1" ]]; then
  echo "Usage: init_cluster.sh ENV"
  exit 1
fi

set -u

env="$1"

helm delete cert-manager --purge || true
echo "Installing Helm"
_helm_install=/volumes/secure/helm.yaml
cat << HERE > "$_helm_install"
apiVersion: v1
kind: ServiceAccount
metadata:
  name: tiller
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: tiller
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  # Use the pre-existing cluster role that comes with GKE.
  name: cluster-admin
subjects:
  - kind: ServiceAccount
    name: tiller
    namespace: kube-system
HERE
kubectl apply -f "$_helm_install"
helm reset --force || true
helm init --service-account tiller

until kubectl -n kube-system get pod -l name=tiller -o jsonpath='{.items[].status.phase}' | grep -q Running; do
  echo "Waiting for Tiller ..."
  sleep 1
done

echo "Deleting all existing keys from the service-accont"
_keys=$(gcloud iam service-accounts keys list \
  --iam-account "cert-manager-$env@nicktravers-site.iam.gserviceaccount.com" \
  --managed-by=user \
  | tail -n+2 \
  | awk '{print $1}')

for _key in $_keys; do
  echo "Deleting key: $_key"
  gcloud iam service-accounts keys delete "$_key" \
    --iam-account "cert-manager-$env@nicktravers-site.iam.gserviceaccount.com" \
    --quiet
done

echo "Creating service-account key"
_key_file=/volumes/secure/service-account.json
gcloud iam service-accounts keys create "$_key_file" \
  --iam-account "cert-manager-$env@nicktravers-site.iam.gserviceaccount.com"

echo "Creating cert-manager namespace"
kubectl delete namespace cert-manager --ignore-not-found=true
kubectl create namespace cert-manager

echo "Creating Secret for cert-manager DNS permissions"
kubectl -n cert-manager delete secret dns --ignore-not-found=true
kubectl -n cert-manager create secret generic dns --from-file "$_key_file"

echo "Setting up cert-manager"
kubectl apply \
  --validate=false \
  -f "https://raw.githubusercontent.com/jetstack/cert-manager/release-$CERT_MANAGER_VERSION/deploy/manifests/00-crds.yaml"
helm repo add jetstack https://charts.jetstack.io
helm repo update
helm install \
  --name cert-manager \
  --namespace cert-manager \
  --version "v$CERT_MANAGER_VERSION" \
  jetstack/cert-manager

while [[ $(kubectl -n cert-manager get pods \
  --field-selector=status.phase=Running \
  --no-headers -o json \
  | jq .items[].metadata.name | wc -l) -ne 3 ]]; do
  echo "Waiting for cert-manager ..."
  sleep 1
done

echo "Completed cluster setup!"
