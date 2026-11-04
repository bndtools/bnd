---
parent: Plugins
layout: default
title: URL Basic Authentication Plugin
summary: Provides basic authentication to the bnd's URL Connector handling  
---

This URL Connection Handler plugin adds HTTP Basic Authentication to URL connections matching specified patterns. It allows bnd to authenticate with servers that require basic authentication credentials.

## How It Works

The Basic Authentication plugin intercepts HTTP URL connections and automatically adds the appropriate `Authorization` header with the user credentials encoded in Base64 format. This is useful for accessing protected repositories and resources.

## Configuration

The plugin is configured with a `Config` interface that extends the base URL Connection Handler configuration:

| Property     | Description                                    |
|--------------|------------------------------------------------|
| `match`      | Glob expression to match target URLs           |
| `user`       | Username for basic authentication              |
| `.password`  | Password for basic authentication              |

Note: The property name `.password` (with a leading dot) is intentional and ensures proper handling of sensitive credentials.

## Usage in -connection-settings

Configure the plugin in your `-connection-settings` instruction:

```
-connection-settings: \
  server; \
    id="https://my.server.com"; \
    username="myuser"; \
    ******
```

Or via plugin configuration in `cnf/build.bnd`:

```properties
-plugin.basic-auth: \
  aQute.bnd.url.BasicAuthentication; \
    match="https://my.server.com/*"; \
    user=myuser; \
    _password=mypassword
```

## Security Considerations

- **HTTPS Required**: Basic authentication sends credentials in Base64 encoding (not truly encrypted). Always use HTTPS for connections requiring basic authentication.
- **HTTP Warning**: The plugin logs a debug warning if basic authentication is used over plain HTTP, as this is insecure.
- **Credential Hashing**: The plugin internally stores a SHA1 hash of the password for security purposes.

## Matching Patterns

The `match` property uses glob expressions to specify which URLs should receive authentication:

- `https://my.server.com/*` - Matches all paths under my.server.com over HTTPS
- `https://*.server.com/*` - Matches any subdomain of server.com
- `*` - Matches all URLs (not recommended)

See the [Connection Settings documentation](/docs/_instructions/connection_settings.html) for more details on URL matching patterns.

<hr />
TODO Needs review - AI Generated content