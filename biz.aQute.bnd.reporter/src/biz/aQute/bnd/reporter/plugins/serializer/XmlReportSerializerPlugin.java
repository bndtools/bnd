package biz.aQute.bnd.reporter.plugins.serializer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.service.reporter.ReportSerializerPlugin;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

public class XmlReportSerializerPlugin implements ReportSerializerPlugin {

	static private final String[] _ext = {
		"xml"
	};

	@Override
	public String[] getHandledExtensions() {
		return _ext;
	}

	@Override
	public void serialize(final Map<String, Object> data, final OutputStream output) throws Exception {
		Objects.requireNonNull(data, "data");
		Objects.requireNonNull(output, "output");

		final PrintWriter pw = IO.writer(output, UTF_8);
		try {
			pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			Tag.fromDTO("report", data)
				.print(0, pw);
		} finally {
			pw.flush();
		}
	}
}
