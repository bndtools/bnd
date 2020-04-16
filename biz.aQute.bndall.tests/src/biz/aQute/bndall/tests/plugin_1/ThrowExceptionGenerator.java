package biz.aQute.bndall.tests.plugin_1;

import java.io.IOException;
import java.util.Optional;

import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;

@ExternalPlugin(name = "throwexception", objectClass = Generator.class)
public class ThrowExceptionGenerator implements Generator<ThrowExceptionGenerator.Empty> {

	interface Empty {}

	@Override
	public Optional<String> generate(BuildContext context, Empty options) throws IOException {
		throw new IllegalStateException();
	}

}
