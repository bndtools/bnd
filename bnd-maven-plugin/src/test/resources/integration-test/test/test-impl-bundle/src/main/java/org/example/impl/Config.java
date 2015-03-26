package org.example.impl;

import static aQute.bnd.annotation.metatype.Meta.*;

import java.io.File;

@OCD
public interface Config {

	@AD
	String hostName();

	@AD
	int port();

	@AD(required = false, deflt = "${user.home}/.cache", description = "Cache directory")
	File cacheDir();

}
