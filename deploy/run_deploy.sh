#!/usr/bin/env bash

set -euo pipefail

_gcp_key_file=${GCP_KEY_FILE:-/deploy/key.json}
_gcp_zone=${GCP_ZONE:-us-central1-a}
_gcp_region=${GCP_ZONE:-us-central1}

_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
_helm_dir="$_dir/helm"

function usage() {
  echo "usage: deploy.sh ENV VERSION"
  echo "ENV: one of 'prod' or 'staging'"
  echo "VERSION: the tag to deploy"
}

if [ ! $# -eq 2 ]; then
  usage
  exit 1
fi

_env="$1"
_version="$2"

_addr_name=site-ingress
_values=values
_cluster=
case "$_env" in
  prod)
    _addr_name="${_addr_name}-production"
    _cluster=site-production
    ;;
  staging)
    _addr_name="${_addr_name}-staging"
    _values="${_values}-staging"
    _cluster=site-staging
    ;;
  *)
    usage
    exit 1
esac

echo "Configuring gcloud ..."
gcloud auth activate-service-account --key-file "$_gcp_key_file"
gcloud config set project "$GCP_PROJECT_ID"
gcloud container clusters get-credentials $_cluster --zone "$_gcp_zone"

echo "Fetching compute address ..."
_addr=$(gcloud compute addresses list \
  --filter "name:$_addr_name AND region:$_gcp_region" \
  --format json | jq .[].address)

echo "Installing Helm chart ..."
helm upgrade blog "$_helm_dir" \
  --values "$_helm_dir/$_values.yaml" \
  --set ipAddress="$_addr" \
  --set version="$_version" \
  --set project="$GCP_PROJECT_ID" \
  --install

echo "Done!"
