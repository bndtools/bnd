package biz.aQute.bnd.reporter.generator;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import aQute.bnd.osgi.WriteResource;
import aQute.bnd.service.reporter.XmlReportPart;
import aQute.lib.io.IO;

public class ReportResource extends WriteResource {
	final List<XmlReportPart> _reportParts;

	public ReportResource(final List<XmlReportPart> reportParts) {
		_reportParts = reportParts;
	}

	@Override
	public void write(final OutputStream out) throws Exception {
		final PrintWriter pw = IO.writer(out, UTF_8);
		try {
			pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

			if (_reportParts.isEmpty()) {
				pw.print("<report />");
			} else {
				pw.print("<report>\n");
				for (XmlReportPart part : _reportParts) {
					part.write(pw);
				}
				pw.print("</report>\n");
			}
		} finally {
			pw.flush();
		}
	}

	@Override
	public long lastModified() {
		return 0;
	}
}
