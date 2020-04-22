package biz.aQute.bndall.tests.plugin_1;

import java.util.Optional;

import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;
import aQute.bnd.service.generate.Options;

@ExternalPlugin(name = "emptyset", objectClass = Generator.class)
public class TestEmptySetGenerator implements Generator<Options> {

	@Override
	public Optional<String> generate(BuildContext context, Options options) {
		return Optional.empty();
	}

}
