package biz.aQute.bndall.tests.plugin_1;

import java.io.IOException;
import java.util.Optional;

import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;

@ExternalPlugin(name = "error", objectClass = Generator.class)
public class ReturnsErrorGenerator implements Generator<ReturnsErrorGenerator.Empty> {

	interface Empty {}

	@Override
	public Optional<String> generate(BuildContext context, Empty options) throws IOException {
		return Optional.of("error");
	}

}
