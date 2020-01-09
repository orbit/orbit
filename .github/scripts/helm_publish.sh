#!/bin/sh

crVersion="v0.2.3"
chartDir="./charts/orbit"
owner="orbit"
repo="orbit.githib.com/orbit"
token=$GITHUB_TOKEN
platform="linux"

curl -sSLo cr.tar.gz "https://github.com/helm/chart-releaser/releases/download/$crVersion/chart-releaser_${crVersion#v}_${platform}_amd64.tar.gz"
tar -xzf cr.tar.gz
rm -f cr.tar.gz

helm package "$chartDir" --destination .cr-release-packages --dependency-update

./cr upload -o "$owner" -r "$repo" -t $token

rm -f cr
