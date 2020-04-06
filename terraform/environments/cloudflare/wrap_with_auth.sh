#!/usr/bin/env bash

set -euo pipefail

_VERSION=1

# Fetch the terraform credentials from gcloud
_token=$(gcloud beta secrets versions access \
  --project=nicktravers-site \
  --secret=cloudflare-api-token "$_VERSION")

# Set the token as the environment variable to use
export CLOUDFLARE_API_TOKEN="$_token"

# Run whatever was passed in.
"$@"
