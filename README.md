# Local Invoices

Local Invoices es una app de escritorio hecha en JavaFX para crear y guardar recibos locales.

La idea de la app es simple: registrar una o varias empresas, crear recibos con datos del cliente, productos o servicios, valores, medio de pago y luego guardar ese historial en el mismo equipo.

Ultimo release:

https://github.com/ReinaMarlon/local-invoices/releases/latest

## Que Hace

- Permite registrar empresas con nombre comercial, razon social, NIT o identificacion, ciudad, pais, telefono y logo.
- Maneja consecutivos de recibos por cada empresa.
- Crea recibos con datos del cliente, medio de pago, observaciones y varios items.
- Calcula automaticamente el total del recibo.
- Guarda la informacion en una base de datos SQLite local.
- Permite consultar recibos guardados desde el historial.
- Copia una imagen del recibo al portapapeles despues de guardarlo.
- Guarda los datos en la carpeta de usuario del sistema operativo.

## Alcance

Esta app sirve para manejar recibos locales o comprobantes internos de pago.

No es un sistema de facturacion electronica. En este momento no se conecta con la DIAN, no genera CUFE, no firma documentos digitalmente y no envia facturas para validacion oficial.

Si en algun momento se quiere convertir en una app de facturacion electronica real, se necesita agregar una integracion aparte con un proveedor o servicio compatible con los requisitos de la DIAN.

## Requisitos Para Usar La App

### Minimos

- Windows 10 de 64 bits.
- 4 GB de RAM.
- 200 MB libres en disco.
- Pantalla de 900 x 620 o superior.
- Permisos para instalar y escribir en la carpeta de usuario.

### Recomendados

- Windows 11 de 64 bits.
- 8 GB de RAM.
- 500 MB libres en disco.
- Pantalla HD o superior.
- Usar la version instalada desde el ultimo release.

La app empaquetada ya incluye su propio runtime de Java, asi que el usuario final no necesita instalar Java manualmente.

## Donde Guarda Los Datos

La base de datos se guarda localmente en el equipo del usuario:

- Windows: `%APPDATA%\Local Invoices\local-invoices.db`
- macOS: `~/Library/Application Support/Local Invoices/local-invoices.db`
- Linux: `~/.local/share/local-invoices/local-invoices.db`

Los logos de las empresas se guardan en una carpeta `logos` dentro de esa misma ruta.

## Stack

- Java 21
- JavaFX 21
- Maven
- SQLite
- jpackage para crear instaladores nativos

## Como Correr En Desarrollo

Para correr la app localmente:

```bash
mvn javafx:run
```

Para generar la imagen runtime:

```bash
mvn -DskipTests javafx:jlink
```

La imagen se genera en:

```text
target/LocalInvoices
```

## Como Se Publican Versiones

Los instaladores se generan desde GitHub Actions cuando se sube un tag con formato:

```text
v1.0.3
```

El workflow toma esa version y la usa para el `.exe`, el `.msi` y la version que se muestra dentro de la app.

## Estructura General

- `domain.model`: modelos principales como empresas, recibos e items.
- `domain.service`: logica de uso de la app.
- `infrastructure.persistence`: conexion SQLite y repositorios.
- `io.github.reinamarlon.localinvoices`: entrada principal de JavaFX y pantallas.

La estructura busca mantener la app ordenada sin complicarla demasiado. La UI esta en JavaFX y la persistencia se maneja con SQLite local.
