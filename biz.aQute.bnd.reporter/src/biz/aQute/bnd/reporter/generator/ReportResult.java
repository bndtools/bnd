package biz.aQute.bnd.reporter.generator;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import aQute.bnd.service.reporter.XmlReportPart;
import aQute.lib.tag.Tag;

public class ReportResult implements XmlReportPart {

	final List<Tag> _tags = new LinkedList<>();

	@Override
	public void write(final PrintWriter out) throws Exception {
		for (final Tag tag : _tags) {
			tag.print(2, out);
		}
	}

	public void add(final Tag tag) {
		_tags.add(tag);
	}

	public void addAll(final List<Tag> tags) {
		_tags.addAll(tags);
	}
}
