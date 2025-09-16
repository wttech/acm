#!/bin/bash

set -e

# Configuration

ACM_VERSION=${ACM_VERSION:-"0.9.46"}
PROJECT_NAME="acme"

xml_append_block_if_missing() {
  local file="$1"
  local marker_id="$2"
  local insert_after="$3"
  local block="$4"
  local marker="<!-- $marker_id -->"
  local found=0

  if ! grep -q "$marker" "$file"; then
    while IFS= read -r line; do
      echo "$line"
      if [[ $found -eq 0 && "$line" == *"$insert_after"* ]]; then
        printf "%s\n%s\n" "$marker" "$block"
        found=1
      fi
    done < "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  fi
}

print_step() {
  echo
  echo "==================================================================="
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
  echo "==================================================================="
  echo
}

print_step "Project setup started, cleaning up previous project (if any)"
rm -vfr "$PROJECT_NAME"

print_step "Generating AEM project '$PROJECT_NAME' using archetype"

mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.3.1:generate \
 -D archetypeGroupId=com.adobe.aem \
 -D archetypeArtifactId=aem-project-archetype \
 -D archetypeVersion=54\
 -D appTitle="ACME" \
 -D appId="$PROJECT_NAME" \
 -D groupId="com.acme"

cd "$PROJECT_NAME"

git init 
git add -A
git commit -m "Initial commit"

print_step "Setting up AEM Compose"
curl https://raw.githubusercontent.com/wttech/aemc/main/project-install.sh | sh
git add -A
git commit -m "AEM Compose setup"

print_step "Adding ACM dependency and embedded to 'all/pom.xml'"

ALL_POM="all/pom.xml"

ACM_DEPENDENCY="    <dependency>
      <groupId>dev.vml.es</groupId>
      <artifactId>acm.all</artifactId>
      <version>$ACM_VERSION</version>
      <type>zip</type>
    </dependency>"

ACM_EMBEDDED="      <embedded>
        <groupId>dev.vml.es</groupId>
        <artifactId>acm.all</artifactId>
        <type>zip</type>
        <target>/apps/acme-packages/application/install</target>
      </embedded>"

xml_append_block_if_missing "$ALL_POM" "acm-all-dependency" "<dependencies>" "$ACM_DEPENDENCY"
xml_append_block_if_missing "$ALL_POM" "acm-all-embedded" "<embeddeds>" "$ACM_EMBEDDED"

print_step "Project setup completed"