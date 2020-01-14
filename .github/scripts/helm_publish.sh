#!/bin/bash

helmVersion="3.0.2"
chartDir="./charts/orbit"
owner="orbit"
repo="orbit"
token=$GITHUB_TOKEN
platform="linux"
version=$TAG_VERSION
author="Build System <orbit@testemail.address>"

curl -sSLo helm.tar.gz https://get.helm.sh/helm-v$helmVersion-$platform-amd64.tar.gz
tar -xzf helm.tar.gz
rm -f helm.tar.gz

cat >| charts/orbit/Chart.yaml << EOF
apiVersion: v1
appVersion: "$version"
description: An Orbit Mesh Helm chart for Kubernetes
name: orbit
version: $version
EOF

mkdir .helm-release-packages
./$platform-amd64/helm package "$chartDir" --destination .helm-release-packages --dependency-update

. ./.github/scripts/upload_chart.sh owner=$owner repo=$repo tag=v$version filename=./.helm-release-packages/orbit-$version.tgz github_api_token=$token

git add ./charts/orbit/Chart.yaml
git commit -m "Bump Helm chart version to $version" --author="$author"

git checkout gh-pages --merge
helm repo index . --merge index.yaml

git add ./index.yaml
git commit -m "Release $version" --author="$author"

rm -rf ./$platform-amd64
rm -rf .helm-release-packages

git reset --hard
git push origin gh-pages
git checkout master