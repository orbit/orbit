#!/bin/bash

helmVersion="3.0.2"
chartDir="./charts/orbit"
owner="orbit"
repo="orbit"
token=$GITHUB_TOKEN
platform="linux"
version=$TAG_VERSION
userEmail="orbit@ea.com"
userName="orbit-tools"
author="$userName <$userEmail>"

git config --global user.email "$userEmail"
git config --global user.name "$userName"
git fetch

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

./$platform-amd64/helm package "$chartDir" --destination . --dependency-update

. ./.github/scripts/upload_chart.sh owner=$owner repo=$repo tag=v$version filename=./orbit-$version.tgz github_api_token=$token

git add ./charts/orbit/Chart.yaml

git checkout -b master --track origin/master --merge
git commit -m "Bump Helm chart version to $version" --author="$author"
git push origin master

git checkout gh-pages --merge
helm repo index . --url https://github.com/orbit/orbit/releases/download/v$version --merge index.yaml

git add ./index.yaml
git commit -m "Release $version" --author="$author"

rm -rf ./$platform-amd64

git reset --hard
git push origin gh-pages
git checkout master