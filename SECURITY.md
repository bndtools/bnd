# bnd Security Model

## Overview
bnd is a build-time tool used to generate and assemble OSGi bundles. As part of this role, bnd processes configuration files (such as .bnd, .bndrun) that define how bundles are built.
bnd configuration files are build instructions, not passive data files. They may invoke macros, plugins, file operations, external commands, or other build-time behavior. Therefore, bnd treats these files as trusted project input and executes them with the privileges of the process running bnd.


## Trust Model
bnd follows the trust model similar to many build tools such as:
- Gradle
- Apache Maven

## Key assumption
bnd operates under the standard build-system trust model: All .bnd files, .bndrun files, workspace configuration, and other build inputs are treated as fully trusted executable instructions provided by the user or their project.
bnd does not sandbox or restrict execution of build instructions. All processing occurs with the full privileges of the invoking process.

## Code Execution During Build
bnd supports a powerful macro system that allows dynamic configuration of bundles.
Some macros, such as ${system}, can:
- execute external system commands
- access the file system
- influence build outputs dynamically

Other macros (e.g., ${env}, file inclusion, or custom processor plugins) can also influence the build dynamically or access external resources.

### Important
Using bnd to build a project may result in arbitrary command execution on the host system (including running shell commands, modifying files, etc.), depending on the contents of the .bnd and .bndrun files.
This behavior is intentional and consistent with typical build tools.

## No Sandbox
bnd does not provide a security sandbox for:
- macro execution
- external command invocation
- file system access

All operations are performed with the **same permissions and privileges** as the user (or process) running the build.

## Working with Untrusted Content
Users should exercise caution when working with:
- third-party templates
- example projects from unknown sources
- externally provided .bnd files

## Risks
Importing or building such content may:
- execute system commands
- modify local files
- access network resources (via external commands)

## Template Import Safeguards
when importing remote or third-party template fragments, bnd:
- identifies content not originating from trusted sources (currently the `bndtools` github organistation is the only trusted source )
- displays a warning to the user
- requires explicit confirmation before fetching content

This ensures that users make an informed trust decision before proceeding with potentially unsafe build instructions.

## Recommended Practices

To reduce risk when using bnd:

**Review build configuration**
- Inspect .bnd files before building
- Look for macros that execute commands (e.g., ${system})

**Use isolated environments**
- Run builds in containers or virtual machines
- Avoid running builds with elevated privileges
- Run with least privilege: Never run bnd builds as root/admin unless strictly necessary. Use dedicated build users or containers.

**Limit trust**
- Only import templates from trusted sources
- Be cautious with third-party repositories

## Non-Goals

bnd does not attempt to:
- prevent execution of malicious build instructions
- validate the safety of .bnd files
- enforce a permission model for macros

These concerns are considered outside the scope of a build tool and are the responsibility of the user and their environment.

## Summary
.bnd files are executable build configurations
- bnd executes with full user privileges
No sandboxing is provided
- Users must trust all build inputs
