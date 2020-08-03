#!/bin/bash

version=$TAG_VERSION
GH_REPO="https://api.github.com/repos/orbit/orbit"
AUTH="Authorization: token $GITHUB_TOKEN"

echo Commit all changed work
git commit -am "Release version $version and update docs" --author="orbit-tools <orbit@ea.com>"

echo Create release notes from previous commit messages
releaseNotes=$(curl -sH "$AUTH" "$GH_REPO/commits?since=$(curl -sH "$AUTH" "$GH_REPO/releases" | jq ".[0].created_at")" | jq ".[].commit.message")
echo Release Notes: $releaseNotes

echo Tag commit with the release tag
git tag v$version
git push origin master --tags

echo Get commit id
commitId=$(git rev-parse HEAD)

releaseData="{\"tag_name\": \"v$version\", \"target_commitish\": \"$commitId\", \"name\":\"Version $version\", \"draft\": false, \"prerelease\": false, \"body\": \"$releaseNotes\" }"

echo Create release
curl -X POST -H "$AUTH" -H "Content-Type: application/json" $GH_REPO/releases -d "$releaseData"

git reset --hard
