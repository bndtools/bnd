package biz.aQute.bnd.proxy.generator;

import java.io.File;
import java.util.Optional;

import aQute.bnd.service.generate.Options;

public interface FacadeSourceGenOptions extends Options {
	Optional<File> output();

	String extend();

	String package_();
	
}
