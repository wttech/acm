#!/bin/bash

set -e

# ===[ Configuration ]===

ACM_VERSION=${ACM_VERSION:-"0.9.46"}
PROJECT_NAME="acme"

print_step() {
  echo
  echo "==================================================================="
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
  echo "==================================================================="
  echo
}

package_append_to_all() {
  local groupId="$1"
  local artifactId="$2"
  local version="$3"
  local type="$4"
  local dep_marker_id="package-${artifactId}-dependency"
  local emb_marker_id="package-${artifactId}-embedded"
  local dep_block="    <dependency>
      <groupId>${groupId}</groupId>
      <artifactId>${artifactId}</artifactId>
      <version>${version}</version>
      <type>${type}</type>
    </dependency>"
  local emb_block="      <embedded>
        <groupId>${groupId}</groupId>
        <artifactId>${artifactId}</artifactId>
        <type>${type}</type>
        <target>/apps/${PROJECT_NAME}-packages/application/install</target>
      </embedded>"
  local all_pom="all/pom.xml"

  echo "Appending package '$groupId:$artifactId:$version' to '$all_pom'"

  xml_append_block_if_missing "$all_pom" "$dep_marker_id" "<dependencies>" "$dep_block"
  xml_append_block_if_missing "$all_pom" "$emb_marker_id" "<embeddeds>" "$emb_block"
}

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

# ===[ Main script ]===

print_step "Project '$PROJECT_NAME' setup started, cleaning up previous project (if any)"
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
sh aemw project scaffold
git add -A
git commit -m "AEM Compose setup"

print_step "Setting up ACM in the project"

package_append_to_all "dev.vml.es" "acm.all" "$ACM_VERSION" "zip"
package_append_to_all "dev.vml.es" "acm.ui.content.example" "$ACM_VERSION" "zip"

git add -A
git commit -m "ACM packages added to all/pom.xml"

print_step "Project '$PROJECT_NAME' setup completed"

