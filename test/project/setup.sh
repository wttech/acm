#!/bin/bash

set -e

# ===[ Configuration ]===

if [ -z "${ACM_VERSION}" ]; then
  VERSION_TAG=$(cd ../../ && git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")
  ACM_VERSION="${VERSION_TAG#v}"
  export ACM_VERSION
  echo "Auto-detected ACM_VERSION: ${ACM_VERSION}"
else
  echo "Using specified ACM_VERSION: ${ACM_VERSION}"
fi

PROJECT_NAME="acme"

print_step() {
  echo
  echo "==================================================================="
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
  echo "==================================================================="
  echo
}

print_error() {
  echo
  echo "ERROR: $1" >&2
  if [ $# -gt 1 ]; then
    echo "$2" >&2
    if [ $# -gt 2 ]; then
      echo "$3" >&2
    fi
  fi
  echo >&2
}

# https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/aem-project-content-package-structure
add_vendor_package() {
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
        <target>/apps/${PROJECT_NAME}-vendor-packages/application/install</target>
      </embedded>"
  local all_pom="all/pom.xml"

  echo "Appending package '$groupId:$artifactId:$version' to '$all_pom'"

  xml_append_block_if_missing "$all_pom" "$dep_marker_id" "</dependencies>" "$dep_block"
  xml_append_block_if_missing "$all_pom" "$emb_marker_id" "</embeddeds>" "$emb_block"
}

xml_append_block_if_missing() {
  local file="$1"
  local marker_id="$2"
  local insert_before="$3"
  local block="$4"
  local marker="<!-- $marker_id -->"
  local found=0

  if ! grep -q "$marker" "$file"; then
    while IFS= read -r line; do
      if [[ $found -eq 0 && "$line" == *"$insert_before"* ]]; then
        printf "%s\n%s\n%s\n" "$marker" "$block" "$line"
        found=1
      else
        echo "$line"
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

add_vendor_package "dev.vml.es" "acm.all" "$ACM_VERSION" "zip"
add_vendor_package "dev.vml.es" "acm.ui.content.example" "$ACM_VERSION" "zip"

git add -A
git commit -m "ACM packages added to all/pom.xml"

if [ -n "${ACM_SLACK_WEBHOOK_URL}" ]; then
  print_step "Configuring Slack notifications"
  
  SLACK_CONFIG=$(cat << EOF
enabled=B"true"
webhookUrl="${ACM_SLACK_WEBHOOK_URL}"
timeoutMillis=I"10000"
id="acm"
EOF
)
  mkdir -p "ui.config/src/main/content/jcr_root/apps/acme/osgiconfig"
  echo "$SLACK_CONFIG" > ui.config/src/main/content/jcr_root/apps/acme/osgiconfig/config/dev.vml.es.acm.core.notification.slack.SlackFactory.acm.config
  
  git add -A
  git commit -m "Slack notification configuration added"

fi

if [ -n "${AEM_CM_URL}" ]; then
  print_step "Pushing to Adobe Cloud Manager"
  git remote add adobe "${AEM_CM_URL}"
  git push adobe main -f

  echo ""
  echo "Build should start automatically in Adobe Cloud Manager if pipeline trigger is configured to 'On Git Changes'."
  echo "If not, now you can start it manually in Adobe Cloud Manager UI."
  echo ""
fi

print_step "Project '$PROJECT_NAME' setup completed"
