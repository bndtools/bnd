---
parent: Plugins
layout: default
title: URL HTTPS Verification Plugin
summary: Verfifies that an HTTPS connection can be trusted  
---

This URL Connection Handler plugin allows you to override the default HTTPS verification behavior. It can be used to specify custom trusted certificates, disable hostname verification, or implement custom certificate validation logic.

## How It Works

The HTTPS Verification plugin:

1. Intercepts HTTPS connections matching the specified URL patterns
2. Initializes a custom SSL context with specified trust managers
3. Applies the custom SSL socket factory to the connection
4. Optionally disables hostname verification for self-signed or internal certificates

## Configuration

The plugin is configured with a `Config` interface:

| Property    | Description                                           |
|-------------|-------------------------------------------------------|
| `match`     | Glob expression to match target URLs                  |
| `trusted`   | Comma-separated paths to X.509 certificate files      |

## Trust Certificates

To specify custom trusted certificates:

```properties
-plugin.https-verify: \
  aQute.bnd.url.HttpsVerification; \
    match="https://my.server.com/*"; \
    trusted=/path/to/cert1.cer,/path/to/cert2.cer
```

The certificates must be in X.509 format (typically with `.cer` extension).

## Disabling Hostname Verification

**WARNING**: Disabling hostname verification removes important security checks. Only use this for testing or with trusted internal servers.

To disable hostname verification:

```properties
-plugin.https-verify: \
  aQute.bnd.url.HttpsVerification; \
    match="https://internal.example.com/*"; \
    hostnameVerify=false
```

## Certificate Chain Validation

When you provide custom certificates:

1. Each certificate is loaded and added to the trust store
2. Missing certificate files generate warnings but don't fail the build
3. The trust manager validates the server's certificate chain against the provided certificates

## SSL Context Initialization

The plugin:

1. Creates a custom SSL context using TLS
2. Registers the trust managers with the context
3. Uses a `SecureRandom` for cryptographic operations
4. Caches the SSL socket factory for performance

## Integration with Connection Settings

This plugin works with bnd's connection settings system. For servers configured in your `-connection-settings`:

```properties
-connection-settings: \
  server; \
    id="https://internal.server.com"; \
    trust=/path/to/internal.cer
```

The HTTPS Verification plugin will apply the custom trust configuration automatically.

## Use Cases

- **Self-Signed Certificates**: Trust a self-signed certificate for an internal server
- **Internal CAs**: Specify your organization's internal Certificate Authority
- **Testing**: Temporarily disable hostname verification during development
- **Custom Trust Chains**: Provide intermediate certificates to complete the trust chain

<hr />
TODO Needs review - AI Generated content