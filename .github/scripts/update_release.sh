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
commitId=$(git rev-parse --short HEAD)
echo Commit Id: $commitId

# Read asset tags.
release=$(curl -sH "$AUTH" "$GH_REPO/releases/tags/_$tag")

echo $release

releaseId=$(jq .id <(cat <<<"$release"))
releaseName=$(jq .name <(cat <<<"$release"))
releaseBody=$(jq .body <(cat <<<"$release"))

echo Release: $releaseId - $releaseName - $releaseBody

# Patch release with new commit Id and tag
curl -X PATCH -H "$AUTH" -H "Content-Type: application/json" $GH_REPO/releases/$releaseId -d '{"tag_name": "$tag", "target_commitish": "$commitId", "name":"$releaseName", "body": "$releaseBody", "draft": "false", "prerelease": "false"}'

git tag -d _$tag
git push origin :refs/tags/_$tag
git reset --hard
