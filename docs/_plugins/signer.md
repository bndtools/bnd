---
parent: Plugins
layout: default
title: Signer Plugin
summary: Provides the capability to sign files 
---

This plugin provides JAR file signing capabilities using the Java `jarsigner` tool. It allows you to sign bundles as part of the bnd build process, ensuring code authenticity and integrity.

## How It Works

The Signer plugin uses the `jarsigner` command-line tool to sign JAR files. During the signing process, it:

1. Creates a temporary copy of the JAR file
2. Invokes the jarsigner tool with your configured signing parameters
3. Copies the signing-related manifest entries and signature files back to the original JAR
4. Preserves the original manifest structure

## Configuration

The plugin is configured with a `Config` interface that supports the following properties:

| Property        | Description                                           | Default      |
|-----------------|-------------------------------------------------------|--------------|
| `keystore`      | Path to the keystore file containing signing keys     | Required     |
| `storetype`     | Keystore type (e.g., JKS, PKCS12)                    | JKS          |
| `storepass`     | Password for accessing the keystore                   | (empty)      |
| `keypass`       | Password for the private key                          | (empty)      |
| `path`          | Path to the jarsigner tool                           | jarsigner    |
| `sigFile`       | Name prefix for signature files                       | (empty)      |
| `digestalg`     | Digest algorithm (e.g., SHA-256)                     | (empty)      |
| `tsa`           | URL of Time Stamping Authority for timestamping      | (empty)      |
| `tsacert`       | TSA certificate alias                                 | (empty)      |
| `tsapolicyid`   | TSA policy identifier                                 | (empty)      |

## Time Stamping Authority (TSA)

For production use, it is recommended to configure a Time Stamping Authority (TSA). This ensures that the signature includes a trusted timestamp, allowing the signature to remain valid even after the signing certificate expires.

## Usage in build.bnd

To enable the Signer plugin in your build configuration:

```properties
-plugin.Signer: \
  aQute.bnd.signing.JartoolSigner; \
    keystore=path/to/keystore.jks; \
    storepass=keystorepassword; \
    keypass=keypassword; \
    sigFile=MySignature
```

Then sign specific bundles by adding the `-sign` instruction to your project's `bnd.bnd` file:

```properties
-sign: alias_name
```

<hr />
TODO Needs review - AI Generated content