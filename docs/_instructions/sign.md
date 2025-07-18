---
layout: default
title: -sign PARAMETERS
class: Project
summary: |
   Report any entries that were added to the build since the last JAR was made.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-sign=alias`

- Values: `<alias> [ ';' 'password:=' <password> ] [ ';'* 'keystore:=' <keystore> ] [ ';' 'sign-password:=' <pw> ] ( ',' ... )*`

- Pattern: `.*`

<!-- Manual content from: ext/sign.md --><br /><br />


# -sign

The `-sign` instruction is used to sign the generated JAR file with a specified certificate or keystore. This is important for distributing secure and trusted bundles, especially in environments that require signed artifacts.

Example:

```
-sign: mykeystore;alias=myalias
```

This instruction ensures that the output JAR is cryptographically signed as part of the build process.

A more detailed example using additional properties:

```
-sign: mykeystore;alias=myalias;storepass=secret;keypass=secret;keystore=path/to/keystore.jks;digestalg=SHA-256
```

In this example:
- `mykeystore` is the name of the keystore.
- `alias` specifies the certificate alias to use for signing.
- `storepass` and `keypass` provide the passwords for the keystore and key.
- `keystore` gives the path to the keystore file.
- `digestalg` sets the digest algorithm (e.g., SHA-256) for the signature.

These options allow you to fully control the signing process and meet the requirements of your deployment environment.




TODO Needs review - AI Generated content
