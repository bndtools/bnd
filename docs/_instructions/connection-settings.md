---
title: -connection-settings
layout: default
summary: Setting up the communications for bnd
---

This chapter discusses the connection settings of bnd that are used when bnd is asked to download or upload a file
from a remote server. The connection settings can provide a userid/password for basic authentication, proxies, and/or
the trust policy for verifying the host name of a TLS/SSL server. These settings can obviously not be part of the workspace 
since they are unique to the actual  user of the workspace. That is, they need to be stored outside the workspace
in a user accessible area.

Since Maven is very popular we've followed the same syntax for settings. The supported elements in this file are 
`<server>` and `<proxy>`. Other elements are ignored.

## Finding the Settings

The default order in which bnd looks for settings is:

	`~/.bnd/connection-settings.xml`
	`~/.m2/settings.xml`

If you want to disable the use of default from the workspace then you can use the `-connection-settings` instruction
in the workspace's `cnf/build.bnd` file. If you set the instruction to `false` then it will not look for the aforementioned files.

	-connection-settings: false

In this setting, you can also list additional files to parse that must be of the same syntax as the Maven settings file. The names 
`maven` and `bnd` are recognized as `~/.m2/settings.xml` and `~/.bnd/connection-settings.xml` respectively. For example, if
we first want to look in the home directory in `~/foo/settings.xml` and then in the bnd settings, we can set the `-connection-settings` to: 

	-connection-settings: ~/foo/settings.xml, bnd

In addition, you could also specify the maven settings inline. For example:

	-connection-settings: \
	   server; \
	       id="http://server"; \
	       username="myUser"; \
	       password=${env;PASSWORD}

## Logging

You can create a log file specific for the connections by specifying:

	-connection-log: somefile.txt

This file will contain the detailed trace output

## Syntax

The settings files have the following XML structure:

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

In maven, the servers are identified by an _id_; when you define a repository you tell which id to use. In bnd this works 
slightly different. Instead of using the id we use the id in the settings as a _glob_ expression on the protocol, host name, 
and port number.  The glob expression must match the scheme, the host and the port if the port is not the default port. 
To match the url `https://maven2-repository.dev.java.net/`, the server component could look like:

	<server>
		<id>https://*java.net</id>
		...
	</server>

The first server that matches the id will provide the parameters.

| Tag               | Default      | Values         | Description                               |
|-----------------------------------------------------------------------------------------------|
| `id`              |              | `GLOB`         | A glob expression on the scheme + host + port   |
| `username`        |              |                | User name for authentication to the server |
| `password`        |              |                | Password for authentication to the server  |
| `verify`          |  `true`      | `false | true` | Enable/disable the verification of the host name against a certificate for HTTPS |
| `trust`           |              |                | Provide paths to certificate that provide trust to the host certificate. The format most of a X.509 certificat file. Normally the extension is `.cer` |

## oAuth2 authentication

It also supports Bearer (OAuth2) authentication. If the `<server>` configuration has only a password and no username, then Bearer authentication is in effect with the password used as the token.

    -connection-settings: server;id="https://*.server.com";password="oauth2token"

will cause

    Authorization: Bearer oauth2token

request header to be sent to servers matching the glob https://*.server.com.

See https://github.github.com/maven-plugins/site-plugin/authentication.html for an example of a `<server>` configuration for OAuth2.

## Commands

The bnd command line provides a number of commands to display and verify the settings as well as getting the information from
getting a remote file.
