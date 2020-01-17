#!/bin/bash

helmVersion="3.0.2"
chartDir="./charts/orbit"
owner="orbit"
repo="orbit"
token=$GITHUB_TOKEN
platform="linux"
version=$TAG_VERSION
indexLocation=".github/pages/index.yaml"

curl -sSLo helm.tar.gz https://get.helm.sh/helm-v$helmVersion-$platform-amd64.tar.gz
tar -xzf helm.tar.gz
rm -f helm.tar.gz

./$platform-amd64/helm package "$chartDir" --destination . --dependency-update

. ./.github/scripts/upload_chart.sh owner=$owner repo=$repo tag=v$version filename=./orbit-$version.tgz github_api_token=$token

git add ./charts/orbit/Chart.yaml

helm repo index . --url https://github.com/orbit/orbit/releases/download/v$version --merge $indexLocation
mv -f index.yaml $indexLocation
git add $indexLocation

rm -rf ./$platform-amd64
