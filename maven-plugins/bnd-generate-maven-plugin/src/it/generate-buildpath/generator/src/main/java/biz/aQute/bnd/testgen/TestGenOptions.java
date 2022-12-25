package biz.aQute.bnd.testgen;

import java.io.File;
import java.util.Optional;

import aQute.bnd.service.generate.Options;

public interface TestGenOptions extends Options {
	Optional<File> output();
}
