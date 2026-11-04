---
parent: Plugins
layout: default
title: URL bnd Authentication Plugin
summary: Provides bnd authentication to the bnd's URL Connector handling  
---

This URL Connection Handler plugin adds bnd's delegated authentication mechanism to HTTP connections. It provides a more sophisticated authentication method based on RSA digital signatures and email identity, making it suitable for secure inter-server communication and API access.

## How It Works

The bnd Authentication plugin uses bnd's built-in delegated authentication system to add signed authentication headers to HTTP requests. It:

1. Retrieves or generates RSA key pairs for the authenticated user
2. Creates an identity string containing the user's email, machine name, and public key
3. Signs the HTTP Date header using the private key
4. Adds a custom `X-aQute-Authorization` header containing the identity and signature

This approach is more secure than basic authentication as it uses cryptographic signing.

## Configuration

The plugin is configured with a `Config` interface that extends the base URL Connection Handler configuration:

| Property      | Description                                                    |
|---------------|----------------------------------------------------------------|
| `match`       | Glob expression to match target URLs                           |
| `email`       | Email address of the account holder                            |
| `publicKey`   | Hex-encoded RSA public key                                     |
| `privateKey`  | Hex-encoded RSA private key (PKCS8 format)                     |
| `machine`     | Machine name for documentation (defaults to system hostname)   |

## Getting Credentials

If no explicit credentials are provided in the configuration, the plugin attempts to use bnd's settings system (see [Settings](/docs/_instructions/settings.html)):

```properties
-plugin.bnd-auth: \
  aQute.bnd.url.BndAuthentication; \
    match="https://my.server.com/*"
```

This will automatically use credentials from your bnd settings file.

## Using Explicit Credentials

For explicit configuration:

```properties
-plugin.bnd-auth: \
  aQute.bnd.url.BndAuthentication; \
    match="https://my.server.com/*"; \
    email=user@example.com; \
    publicKey=<hex-encoded-public-key>; \
    privateKey=<hex-encoded-private-key>
```

## Security Considerations

- **HTTPS Required**: The plugin logs a debug warning if used over plain HTTP. Bnd authentication should only be used with HTTPS.
- **Date Signing**: The plugin signs the HTTP Date header. If no Date header is present, it creates one with the current time.
- **Public Key Transport**: The public key is transmitted with each request for server verification purposes. Keep your private key secure.

## Authorization Header Format

The `X-aQute-Authorization` header follows this format:

```
X-aQute-Authorization: <email>!<machine>!<base64-public-key>:<base64-signature>
```

Where:
- `<email>` - User's email address
- `<machine>` - Machine name (for documentation/audit purposes)
- `<base64-public-key>` - The RSA public key in Base64 format
- `<base64-signature>` - SHA1withRSA signature of the Date header

## Complete Example

To set up bnd authentication for a private repository:

1. Configure the plugin in `cnf/build.bnd`:

```properties
-plugin.bnd-auth: \
  aQute.bnd.url.BndAuthentication; \
    match="https://repo.example.com/*"; \
    email=developer@example.com; \
    publicKey=3082010a0282010100aabb....; \
    privateKey=308204a30201000282010100aabb....
```

2. Or use your bnd settings file for automatic credentials:

```properties
-plugin.bnd-auth: \
  aQute.bnd.url.BndAuthentication; \
    match="https://repo.example.com/*"
```

3. When bnd makes requests to `https://repo.example.com/*`, the authorization header will be automatically added with a signed timestamp.

<hr />
TODO Needs review - AI Generated content