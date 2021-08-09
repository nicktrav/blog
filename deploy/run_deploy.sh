#!/usr/bin/env bash

set -euo pipefail

_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
_helm_dir="$_dir/helm"

_token=$DIGITAL_OCEAN_TOKEN # GitHub secret.

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

_values=values
_cluster=
case "$_env" in
  prod)
    _cluster="site-production"
    ;;
  staging)
    _values="${_values}-staging"
    _cluster="site-staging"
    ;;
  *)
    usage
    exit 1
esac

echo "Generating kubeconfig ..."
mkdir -p ~/.kube
doctl \
  --access-token="$_token" \
  kubernetes cluster kubeconfig show "$_cluster" > ~/.kube/config

echo "Installing Helm chart ..."
helm upgrade site "$_helm_dir" \
  --values "$_helm_dir/$_values.yaml" \
  --set project="$GCP_PROJECT_ID" \
  --set version="$_version" \
  --install

echo "Done!"
