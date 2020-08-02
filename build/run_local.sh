#!/usr/bin/env bash

set -euo pipefail

_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo "Building container ..."
docker build -t site -f "$_dir/Dockerfile-local" .

echo "Running ..."
docker run --rm -it \
  -v "$_dir/../content:/site" \
  -p 3000:3000 \
  --network=host \
  site
