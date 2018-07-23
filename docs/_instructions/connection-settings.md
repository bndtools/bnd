---
title: -connection-settings
layout: default
summary: Setting up the communications for bnd
---

This chapter discusses the communication settings of bnd that you will need when a server requires a proxy, password, or other communication settings. These settings can obviously not be part of the workspace since they are unique to the actual user of the workspace.

Since Maven support is quite widespread we've tried to align the communication settings with the settings of maven. Out of the box, bnd will use the settings in `~/.m2/settings.xml`. We made it out of the box so that people could get a seam less experience when using OSGi workspaces. However, there can reasons to not share the maven settings. In that case you must create a `~/.bnd/connection-settings.xml` file that follows the same syntax as the maven file. The order bnd looks for settings is therefore:

	`~/.bnd/connection-settings.xml`
	`~/.m2/settings.xml`

If you want to disable the use of these mechanisms from the workspace then you can use the `-connection-settings` instruction. If you set this to `false` then it will not look for the aforementioned files. 

	-connection-settings: false

In this setting, you can also list additional files to parse that must be of the same syntax as maven settings. The name `maven` and `bnd` are recognized as `~/.m2/settings.xml` and `~/.bnd/connection-settings.xml` respectively.

	-connection-settings: ~/foo/settings.xml, bnd

Only the `proxy` and `server` elements are looked at and only the in here defined fields.

We provide a number of additional features over the maven syntax.

## Logging out

You can create a log file specific for the connections by specifying:

	-connection-log: somefile.txt

This file will contain the detailed trace output

## Syntax

The settings files have the following structure:

	<settings>
	  <proxies>
	    <proxy>
	    <id/>
	      <active>        'true' | 'false' </active>
	      <protocol>      'http'| 'direct' | 'socks' </protocol>
	      <host>          domain name </host>
	      <port>          port to use </port>
	      <username>      user name to use for the proxy </username>
	      <password>      password to use for the proxy </password>
	      <verify> true | false </verify>
	      <nonProxyHosts> glob ( '|' glob )* </nonProxyHosts> 
	    </proxy>
	  </proxies>
	  <servers>
	    <server>
	      <id>default</id>
		  <username>username</username>
	      <password>password</password>
		  <verify> true | false </verify>
		  <trust> comma separated paths to X509 certificates </trust>
	    </server>
	  </servers>
	</settings> 

Any additional elements are ignored.

### Proxies

The purpose of the proxy definitions is to define a communication proxy. We support `HTTP` and `SOCKS` proxies. An additional built-in proxy is the `DIRECT` 'proxy'. Proxies can be authenticated with a username and password.


| Tag               | Default      | Values         | Description                               |
|-----------------------------------------------------------------------------------------------|
| `id`              | `default`    | `NAME`         | Identifies the proxy, must be unique in   |
|                   |              |                | the list of proxies.                      |
| `active`          | `true`       | `true | false` | If this proxy is active                   |
| `protocol`        | `http`       | `socks | http  | direct` | The proxy protocol to use.      |
| `host`            |              | `domain-name`  | The domain name or IP address of the proxy host |
| `port`            |              | `INTEGER`      | Port number, maybe absent if default      |
|                   |              |                | port for protocol                         |
| `username`        |              |                | User name for authentication to the proxy |
| `password`        |              |                | Password for authentication to the proxy  |
| `nonProxyHosts`   | none         | `GLOB ('|' GLOB)*` | Filter on the destination hosts that should not be proxied                     |

### Servers

In maven, the servers are identified by an id; when you define a repository you tell which id to use. In bnd this works slightly different. Instead of using the id we use the id in the settings as a _glob_ expression on the url. The glob expression must match the scheme, the host and the port if the port is not the default port. To match the url `https://maven2-repository.dev.java.net/`, the server component could look like:

	<server>
		<id>https://*java.net</id>
		...
	</server>

| Tag               | Default      | Values         | Description                               |
|-----------------------------------------------------------------------------------------------|
| `id`              |              | `GLOB`         | A glob expression on the scheme + host + port   |
| `username`        |              |                | User name for authentication to the server |
| `password`        |              |                | Password for authentication to the server  |
| `verify`          |  `true`      | `false | true` | Enable/disable the verification of the host name against a certificate for HTTPS |
| `trust`           |              |                | Provide paths to certificate that provide trus to the host certificate |

	
