#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

rm -f deploy.zip
zip -r deploy.zip Dockerrun.aws.json .platform

echo "Bundle gerado em $(pwd)/deploy.zip"
