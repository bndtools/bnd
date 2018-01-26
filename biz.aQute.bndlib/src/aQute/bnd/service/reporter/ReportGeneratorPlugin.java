package aQute.bnd.service.reporter;

import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;

public interface ReportGeneratorPlugin {
	Tag report(Jar jar) throws Exception;
}
