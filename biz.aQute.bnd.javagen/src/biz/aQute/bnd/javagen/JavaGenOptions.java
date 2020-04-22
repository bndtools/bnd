package biz.aQute.bnd.javagen;

import java.io.File;
import java.util.Optional;

import aQute.bnd.service.generate.Options;

public interface JavaGenOptions extends Options {
	Optional<File> output();
}
