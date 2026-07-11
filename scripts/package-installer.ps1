$ErrorActionPreference = "Stop"

mvn -DskipTests javafx:jlink

$imagePath = Resolve-Path "target/LocalInvoices"
New-Item -ItemType Directory -Force "target/installer" | Out-Null
$javaVersion = [int]((java -version 2>&1 | Select-Object -First 1) -replace '.*version "(\d+).*', '$1')
$runtimeOptions = @("--java-options", "--enable-native-access=javafx.graphics,org.xerial.sqlitejdbc")
if ($javaVersion -ge 23) {
  $runtimeOptions += @("--java-options", "--sun-misc-unsafe-memory-access=allow")
}

jpackage `
  --type msi `
  --name "Local Invoices" `
  --app-version "1.0.0" `
  --vendor "io.github.reinamarlon" `
  --description "Native desktop app for local invoice and receipt management" `
  --runtime-image "$imagePath" `
  --module "io.github.reinamarlon.localinvoices/io.github.reinamarlon.localinvoices.LocalInvoicesApp" `
  @runtimeOptions `
  --dest "target/installer" `
  --win-menu `
  --win-shortcut
