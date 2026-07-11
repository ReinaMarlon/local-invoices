#!/usr/bin/env bash
set -euo pipefail

mvn -DskipTests javafx:jlink

TYPE="deb"
if [[ "$(uname -s)" == "Darwin" ]]; then
  TYPE="dmg"
fi

mkdir -p target/installer

ICON_ARGS=()
if [[ "$TYPE" == "deb" ]]; then
  ICON_ARGS=(--icon "src/main/resources/io/github/reinamarlon/localinvoices/images/icon.png")
fi

RUNTIME_OPTIONS=(--java-options "--enable-native-access=javafx.graphics,org.xerial.sqlitejdbc")
JAVA_VERSION=$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -n 1)
if [[ "$JAVA_VERSION" -ge 23 ]]; then
  RUNTIME_OPTIONS+=(--java-options "--sun-misc-unsafe-memory-access=allow")
fi

jpackage \
  --type "$TYPE" \
  --name "Local Invoices" \
  --app-version "1.0.0" \
  --vendor "io.github.reinamarlon" \
  --description "Native desktop app for local invoice and receipt management" \
  --runtime-image "target/LocalInvoices" \
  --module "io.github.reinamarlon.localinvoices/io.github.reinamarlon.localinvoices.LocalInvoicesApp" \
  "${RUNTIME_OPTIONS[@]}" \
  "${ICON_ARGS[@]}" \
  --dest "target/installer"
