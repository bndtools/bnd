package biz.aQute.bnd.reporter.plugins.resource.converter;

import java.io.InputStream;
import java.util.Objects;

import aQute.lib.json.Decoder;
import aQute.lib.json.JSONCodec;
import biz.aQute.bnd.reporter.service.resource.converter.ResourceConverterPlugin;

public class JsonConverterPlugin implements ResourceConverterPlugin {

	static private final String[]	_ext	= {
		"json"
	};

	private final Decoder			dec		= new JSONCodec().dec()
		.keepOpen();

	@Override
	public String[] getHandledExtensions() {
		return _ext;
	}

	@Override
	public Object extract(final InputStream input) throws Exception {
		Objects.requireNonNull(input, "input");

		return dec.from(input)
			.get();
	}
}
