package biz.aQute.bndall.tests.plugin_1;

import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;

import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;
import aQute.lib.io.IO;

@ExternalPlugin(name = "greport", objectClass = Generator.class)
public class GRreport implements Generator<GRreport.Options> {

	interface Options {
		File file();
	}

	@Override
	public Optional<String> generate(BuildContext context, Options options) throws Exception {
		if (options.file() == null)
			throw new IllegalArgumentException("no file");

		options.file()
			.getParentFile()
			.mkdirs();
		StringBuilder sb = new StringBuilder();

		String s = context.getFlattenedProperties()
			.entrySet()
			.stream()
			.map(e -> (e.getKey() + ": " + e.getValue()))
			.collect(Collectors.joining("\n"));
		sb.append(1);

		IO.store(sb.toString(), options.file());
		return null;
	}
}
