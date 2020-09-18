for line in $@; do
  eval "$line"
done

version=$TAG_VERSION
tag=v$version
owner="orbit"
repo="orbit"
GH_REPO="https://api.github.com/repos/$owner/$repo"
AUTH="Authorization: token $GITHUB_TOKEN"
filename="./orbit-$version.tgz"

# Validate token.
curl -o /dev/null -sH "$AUTH" $GH_REPO || { echo "Error: Invalid repo, token or network issue!";  exit 1; }

# Get release
release=$(curl -sH "$AUTH" $GH_REPO/releases/tags/$tag)

# Extract the release id
releaseId=$(jq .id <(cat <<<"$release"))
[ "$releaseId" ] || { echo "Error: Failed to get release id for tag: $tag"; echo "$release" | awk 'length($0)<100' >&2; exit 1; }

# Upload asset
echo "Uploading asset... "

# Construct upload url
GH_UPLOAD_URL="https://uploads.github.com/repos/$owner/$repo/releases/$releaseId/assets?name=$(basename $filename)"

curl --data-binary @"$filename" -H "$AUTH" -H "Content-Type: application/octet-stream" $GH_UPLOAD_URL
