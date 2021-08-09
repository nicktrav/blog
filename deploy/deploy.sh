#!/usr/bin/env bash

set -euo pipefail

_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
_container_name=site-deploy
_gcp_key_file=${GCP_KEY_FILE:-/dev/shm/key.json}

if [ ! $# -eq 2 ]; then
  usage
  exit 1
fi

_env="$1"
_version="$2"

echo "Building the Deploy container ..."
docker build \
  -t "$_container_name" \
  -f "$_dir/Dockerfile-deploy" "$_dir"

echo "Running deploy ..."
docker run --rm \
  -v "$_gcp_key_file:/deploy/key.json" \
  -v "$_dir/helm:/deploy/helm" \
  -e GCP_PROJECT_ID="$GCP_PROJECT_ID" \
  -e DIGITAL_OCEAN_TOKEN="$DIGITAL_OCEAN_TOKEN" \
  "$_container_name" \
  "$_env" "$_version"
