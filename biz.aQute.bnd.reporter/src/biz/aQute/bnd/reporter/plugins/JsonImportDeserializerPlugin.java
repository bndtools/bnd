package biz.aQute.bnd.reporter.plugins;

import java.io.InputStream;
import java.util.Objects;

import aQute.bnd.service.reporter.ReportImportDeserializerPlugin;
import aQute.lib.json.Decoder;
import aQute.lib.json.JSONCodec;

public class JsonImportDeserializerPlugin implements ReportImportDeserializerPlugin {

	static private final String[] _ext = { "json" };

	private final Decoder dec = new JSONCodec().dec().keepOpen();

	@Override
	public String[] getHandledExtensions() {
		return _ext;
	}

	@Override
	public Object deserialyze(final InputStream input) throws Exception {
		Objects.requireNonNull(input, "input");

		return dec.from(input).get();
	}
}
