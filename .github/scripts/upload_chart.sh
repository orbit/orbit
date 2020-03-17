for line in $@; do
  eval "$line"
done

GH_API="https://api.github.com"
GH_REPO="$GH_API/repos/$owner/$repo"
GH_TAG_URL="$GH_REPO/releases/tags/_$tag"
AUTH="Authorization: token $github_api_token"

# Validate token.
curl -o /dev/null -sH "$AUTH" $GH_REPO || { echo "Error: Invalid repo, token or network issue!";  exit 1; }

# Get release
release=$(curl -sH "$AUTH" $GH_TAG_URL)

# Extract the release id
releaseId=$(jq .id <(cat <<<"$release"))
[ "$releaseId" ] || { echo "Error: Failed to get release id for tag: $tag"; echo "$release" | awk 'length($0)<100' >&2; exit 1; }

# Upload asset
echo "Uploading asset... "

# Construct upload url
GH_UPLOAD_URL="https://uploads.github.com/repos/$owner/$repo/releases/$releaseId/assets?name=$(basename $filename)"

curl --data-binary @"$filename" -H "$AUTH" -H "Content-Type: application/octet-stream" $GH_UPLOAD_URL
