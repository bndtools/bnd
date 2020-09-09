package org.example.impl;

import org.osgi.service.metatype.annotations.*;

import java.io.File;

@ObjectClassDefinition
public interface Config {

	String hostName();

	int port();

	@AttributeDefinition(required = false, defaultValue = "${user.home}/.cache", description = "Cache directory")
	File cacheDir();

}
