owner="orbit"
repo="orbit"

tag=$TAG_VERSION
GH_REPO="https://api.github.com/repos/$owner/$repo"
AUTH="Authorization: token $GITHUB_TOKEN"

# Commit all changed work
git commit -m "Release version $tag and update docs" --author="orbit-tools <orbit@ea.com>"

# Get commit id
commitId=$(git rev-parse HEAD)

# Tag commit with the intended release tag (without the underscore)
git tag "$tag"
git tag -d "_$tag"
git push origin master
git reset --hard

# Read asset tags.
releaseResponse=$(curl -sH "$AUTH" "$GH_REPO/releases/tags/_$tag")

# Extract the release id
eval $(echo "$releaseResponse" | grep -m 1 "id.:" | grep -w id | tr : = | tr -cd '[[:alnum:]]=')
[ "$id" ] || { echo "Error: Failed to get release id for tag: $tag"; echo "$releaseResponse" | awk 'length($0)<100' >&2; exit 1; }

# Construct edit release url
GH_EDIT_RELEASE_URL"$GH_REPO/releases/$id"

# Patch release with new commit Id and tag
curl -X PATCH -H "$AUTH" -H "Content-Type: application/json" $GH_EDIT_RELEASE_URL -d '{"tag_name": "$tag", "target_commitish": "$commitId"}'
