#!/bin/bash

owner="orbit"
repo="orbit"

tag=v$TAG_VERSION
GH_REPO="https://api.github.com/repos/$owner/$repo"
AUTH="Authorization: token $GITHUB_TOKEN"

# Commit all changed work
git commit -m "Release version $tag and update docs" --author="orbit-tools <orbit@ea.com>"

# Tag commit with the intended release tag (without the underscore)
git tag $tag
git push origin master --tags

# Get commit id
commitId=$(git rev-parse HEAD)

# Get release
release=$(curl -sH "$AUTH" "$GH_REPO/releases/tags/_$tag")

releaseId=$(jq .id <(cat <<<"$release"))
[ "$releaseId" ] || { echo "Error: Failed to get release id for tag: $tag"; echo "$response" | awk 'length($0)<100' >&2; exit 1; }

releaseName=$(jq .name <(cat <<<"$release"))

releaseData="{\"tag_name\": \"$tag\", \"target_commitish\": \"$commitId\", \"name\":$releaseName, \"draft\": \"false\", \"prerelease\": \"false\"}"

echo Updating release $releaseId: $releaseData

# Patch release with new commit Id and tag
curl -X PATCH -H "$AUTH" -H "Content-Type: application/json" $GH_REPO/releases/$releaseId -d "$releaseData"

git tag -d _$tag
git push origin :refs/tags/_$tag
git reset --hard
