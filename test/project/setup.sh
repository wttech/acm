#!/bin/bash

set -e

print_step() {
  echo
    echo "==================================================================="
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo "==================================================================="
  echo
}

print_step "Project setup started, cleaning up previous project (if any)"
rm -vfr acme

print_step "Generating AEM project 'ACME' using archetype"

mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.3.1:generate \
 -D archetypeGroupId=com.adobe.aem \
 -D archetypeArtifactId=aem-project-archetype \
 -D archetypeVersion=54\
 -D appTitle="ACME" \
 -D appId="acme" \
 -D groupId="com.acme"

cd acme

git init 

git add -A
git commit -m "Initial commit"

print_step "Setting up AEM Compose"

curl https://raw.githubusercontent.com/wttech/aemc/main/project-install.sh | sh

git add -A
git commit -m "AEM Compose setup"

print_step "Project setup completed"