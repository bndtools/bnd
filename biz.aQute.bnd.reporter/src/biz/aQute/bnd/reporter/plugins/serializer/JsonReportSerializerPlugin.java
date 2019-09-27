package biz.aQute.bnd.reporter.plugins.serializer;

import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.service.reporter.ReportSerializerPlugin;
import aQute.lib.json.Encoder;
import aQute.lib.json.JSONCodec;

public class JsonReportSerializerPlugin implements ReportSerializerPlugin {

	static private final String[]	_ext	= {
		"json"
	};

	private final Encoder			enc		= new JSONCodec().setIgnorenull(true)
		.enc()
		.keepOpen()
		.indent("  ")
		.writeDefaults();

	@Override
	public String[] getHandledExtensions() {
		return _ext;
	}

	@Override
	public void serialize(final Map<String, Object> data, final OutputStream output) throws Exception {
		Objects.requireNonNull(data, "data");
		Objects.requireNonNull(output, "output");

		enc.to(output)
			.put(data)
			.flush();
	}
}
