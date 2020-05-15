version=$TAG_VERSION

sed -i.bak "s/appVersion: .*/appVersion: $version/" charts/orbit/Chart.yaml
sed -i.bak "s/version: .*/version: $version/" charts/orbit/Chart.yaml
rm -f charts/orbit/Chart.yaml.bak
git add ./charts/orbit/Chart.yaml

sed -i.bak "s/tag: 2.*/tag: $version/" charts/orbit/values.yaml
rm -f charts/orbit/values.yaml.bak
git add ./charts/orbit/values.yaml

sed -i.bak "s/orbit.version.*/orbit.version=$version/" gradle.properties 
rm -f gradle.properties.bak
git add gradle.properties

sed -i.bak "s/release.*/release: $version/" .gitbook.yaml
rm -f .gitbook.yaml.bak
git add .gitbook.yaml
