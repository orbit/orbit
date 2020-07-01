#!/bin/bash

owner="orbit"
repo="orbit"

tag=v$TAG_VERSION
GH_REPO="https://api.github.com/repos/$owner/$repo"
AUTH="Authorization: token $GITHUB_TOKEN"

git config user.email "orbit@ea.com"
git config user.name "orbit-tools"

echo Commit all changed work
git commit -am "Release version $tag and update docs" --author="orbit-tools <orbit@ea.com>"

echo Tag commit with the intended release tag without the underscore
git tag $tag
git push origin master --tags

echo Get commit id
commitId=$(git rev-parse HEAD)

echo Get release
release=$(curl -sH "$AUTH" "$GH_REPO/releases/tags/_$tag")

releaseId=$(jq .id <(cat <<<"$release"))
[ "$releaseId" ] || { echo "Error: Failed to get release id for tag: $tag"; echo "$response" | awk 'length($0)<100' >&2; exit 1; }
releaseName=$(jq .name <(cat <<<"$release"))
releaseData="{\"tag_name\": \"$tag\", \"target_commitish\": \"$commitId\", \"name\":$releaseName, \"draft\": \"false\", \"prerelease\": \"false\"}"

echo Patch release with new commit Id: $releaseId and tag: $releaseData
curl -X PATCH -H "$AUTH" -H "Content-Type: application/json" $GH_REPO/releases/$releaseId -d "$releaseData"

git tag -d _$tag
git push origin :refs/tags/_$tag
git reset --hard
