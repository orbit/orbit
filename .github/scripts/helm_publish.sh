#!/bin/bash

helmVersion="3.0.2"
chartDir="./charts/orbit"
owner="orbit"
repo="orbit"
token=$GITHUB_TOKEN
platform="linux"
version=$TAG_VERSION

cat >| charts/orbit/Chart.yaml << EOF
apiVersion: v1
appVersion: "$version"
description: An Orbit Mesh Helm chart for Kubernetes
name: orbit
version: $version
EOF

curl -sSLo helm.tar.gz https://get.helm.sh/helm-v$helmVersion-$platform-amd64.tar.gz
tar -xzf helm.tar.gz
rm -f helm.tar.gz

mkdir .helm-release-packages
./$platform-amd64/helm version
./$platform-amd64/helm package "$chartDir" --destination .helm-release-packages --dependency-update

. ./.github/scripts/upload_chart.sh owner=$owner repo=$repo tag=v$version filename=./.helm-release-packages/orbit-$version.tgz github_api_token=$token

rm -rf ./$platform-amd64