#!/bin/sh

crVersion="0.2.3"
helmVersion="3.0.2"
chartDir="./charts/orbit"
owner="orbit"
repo="orbit.githib.com/orbit"
token=$GITHUB_TOKEN
platform="linux"

curl -sSLo helm.tar.gz https://get.helm.sh/helm-v$helmVersion-linux-386.tar.gz
tar -xzf helm.tar.gz
rm -f helm.tar.gz

curl -sSLo cr.tar.gz "https://github.com/helm/chart-releaser/releases/download/v$crVersion/chart-releaser_$crVersion_${platform}_amd64.tar.gz"
tar -xzf cr.tar.gz
rm -f cr.tar.gz

mkdir .cr-release-packages
./helm version
./helm package "$chartDir" --destination .cr-release-packages --dependency-update --save=false

./cr upload -o "$owner" -r "$repo" -t $token

rm -f cr
rm -rf helm