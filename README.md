# Local Invoices

Native desktop app for local receipt and invoice management.

## Stack

- Java 21
- JavaFX 21
- Maven
- SQLite
- `jpackage` for native installers

## Package Structure

The Java package is `io.github.reinamarlon.localinvoices`.

That package name is a good fit for a GitHub-hosted personal/open-source project because it follows the reverse-domain convention for `github.io/reinamarlon`.

The app is organized by responsibilities:

- `domain.model`: immutable business records such as `Company`, `Bill`, and draft objects.
- `domain.service`: application use cases and validation. This keeps JavaFX from talking directly to repositories.
- `infrastructure.persistence`: SQLite schema and repositories.
- `io.github.reinamarlon.localinvoices`: JavaFX desktop entry point and UI composition.

## Run Locally

```bash
mvn javafx:run
```

On Windows you can also run:

```powershell
.\scripts\run.ps1
```

## Build Runtime Image

```bash
mvn -DskipTests javafx:jlink
```

The runtime image is generated at:

```text
target/LocalInvoices
```

## Create Installer

Windows:

```powershell
.\scripts\package-installer.ps1
```

macOS/Linux:

```bash
./scripts/package-installer.sh
```

Installers are generated in:

```text
target/installer
```

## Local Data

The SQLite database is stored in the current user's application data folder:

- Windows: `%APPDATA%\Local Invoices\local-invoices.db`
- macOS: `~/Library/Application Support/Local Invoices/local-invoices.db`
- Linux: `~/.local/share/local-invoices/local-invoices.db`

Company logos are copied into the same application data folder under `logos/`.

## Current Feature Set

- Manual company registration with commercial name, legal name, tax ID, city, country, phone, and logo/photo
- No preloaded companies; the user creates all companies manually
- Company selector backed by SQLite data
- Consecutive receipt numbers per company
- Detailed local receipt form: customer identification, email, address, payment method, multiple line items, quantities, unit prices, and automatic totals
- SQLite persistence
- Receipt snapshot copied to clipboard after saving
- History view filtered by ID, name, or phone
- Single JavaFX scene so window size/maximized state is preserved while navigating
- Native packaging path for Windows, Linux, and macOS

## Compliance Scope

Local Invoices currently produces local receipts and internal payment records. It is not an electronic invoice provider: it does not request DIAN numbering authorization, apply a digital signature, transmit documents for validation, or issue a CUFE. Those capabilities require a separate DIAN-compliant electronic invoicing integration.
