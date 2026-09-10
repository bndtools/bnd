---

title: Settings
layout: bnd
parent: Configuration and Troubleshooting
nav_order: 1
------------

bnd can be configured from several sources, depending on the scope and purpose of the setting.

Workspace and project configuration should normally be kept with the build so that builds remain reproducible across different machines. Some settings, however, are specific to the local user or environment and should not be stored in the workspace. Examples include credentials, local identity information, and process-level configuration.

For these cases, bnd supports:

* a user-specific settings file at `~/.bnd/settings.json`
* Java system properties
* environment variables

The settings file is intended for persistent user-specific configuration that should remain outside the workspace. System properties and environment variables are useful for configuration supplied by the launcher, build environment, CI system, or operating system.

## User Settings

bnd stores user-specific settings in:

```text
~/.bnd/settings.json
```

The settings file can contain configuration such as identity and authorization information. Depending on the environment, it can also be protected with a password using the `BND_SETTINGS_PASSWORD` environment variable.

The file contains ordinary string settings in `map`, together with the public `id` and private `secret` key data used by
bnd authentication. The [settings sample](../examples/settings.json) shows the complete JSON shape. The key data in that
sample is disposable; generate a new identity for real use with `bnd settings -g`.

## Usage

Copy the sample to the default location, then replace its example values:

    $ mkdir -p ~/.bnd
    $ cp docs/examples/settings.json ~/.bnd/settings.json

Read a value from the settings file with the `global` macro in a workspace `cnf/build.bnd`:

    repository.user: ${global;example.username}

Use a default when the setting is absent:

    repository.token: ${global;example.token;not-configured}

## Authorization

## The bnd settings Command

## The global macro

## Authorizing a New System

The settings automatically generate a private and public key when they are initialized. However, in certain cases it is necessary to explicitly authorize a system. For example, CI environments often start each build on a newly initialized machine. In such a case it may be necessary to explicitly provision the required identity.

On a trusted host, get the current private and public key and the email:

```
$ bnd identity
--publicKey 08E...CF --privateKey C7FE...D3 --email bnd@example.org
```

Copy the output and run the following command on the target system:

```
$ bnd identity <copied output>
```

This configures the target system with the same authorization as the original machine.

If you instead want to create a separate private/public key pair, you can create a temporary settings file:

```
$ bnd settings -l temp.json -g
$ bnd identity -l temp.json
--publicKey 08E...CF --privateKey C7FE...D3 --email bnd@example.org
...
$ bnd identity <copied output>
```

## System Properties and Environment Variables

Some bnd behavior can be configured using Java system properties or environment variables. These are particularly useful for process-wide settings, launcher configuration, and CI environments.

### System Properties

| Property Name             | Description                                                                                                                                                                                                                                                                                                                                     | Default | Type  |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- | ----- |
| `bnd.xml.entitySizeLimit` | Default entity-size limit used by XML parsers created by bnd. The value is used as the fallback for the JAXP `jdk.xml.totalEntitySizeLimit` and `jdk.xml.maxGeneralEntitySizeLimit` properties. A value of `0` means unlimited. The corresponding `jdk.xml.*` system property, when explicitly set, takes precedence for that individual limit. | `0`     | `int` |

### Environment Variables

| Variable Name           | Description                                                                                                            | Example                                         |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- |
| `BND_SETTINGS_PASSWORD` | Password used to encrypt or decrypt `~/.bnd/settings.json`. If it is not set, the settings file is stored unencrypted. | `export BND_SETTINGS_PASSWORD=mysecretpassword` |
