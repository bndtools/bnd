---
title: Settings
layout: default
---

For a build model it is crucial that it has no dependencies on the machine it is running on. One of the  most frustrating developer experiences is the phrase: 'Yes, but it runs on my machine!'. For this reason bnd goes out of it is way to have no dependencies outside its workspace. That is, until we hit security. It is in the nature of security to have secrets and by definitions these secrets cannot be in the workspace, or, well, they will not be that secret for long. Though the best solution is to keep the secret in your head, there are times when this is impossible or plain cumbersome.

For this reason, bnd maintains a settings file in ~/.bnd/settings.json. This file contains ordinary settings ...

TBD

## Authorization

## The bnd settings Command

## The global macro
	

## Authorizing a New System

The settings automatically generate a private and public key when they are initialized. However, in certain cases it is necessary to explicitly authorize a system. For example, [Travis][1] always starts a build from a completely freshly initialized VM. In such a case it is necessary to authorize the machine explicitly. Though it is possible to just copy an existing settings file to this machine, it is more elegant to use the bnd commands. Do the following steps to authorize a machine.

On a trusted host, get the current private and public key and the email:

	$ bnd identity
	--publicKey 08E...CF --privateKey C7FE...D3 --email bnd@example.org

The output is a bit longer than shown because these keys are darned long, you know, security is hard. You should copy the output and then on the target system run:

	$ bnd identity <copied output>

 This will use the same authorization as the original machine. If you want to create a more unique authorization, then it is possible to create a new private/public key pair. 
 
     $ bnd settings -l temp.json -g
     $ bnd identity -l temp.json 
     --publicKey 08E...CF --privateKey C7FE...D3 --email bnd@example.org
     ...
     $ bnd identity <copied output>
	






[1]: https://travis-ci.org/

