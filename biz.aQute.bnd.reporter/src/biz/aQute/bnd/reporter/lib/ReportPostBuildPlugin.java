package biz.aQute.bnd.reporter.lib;

import java.util.Map;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.service.buildevents.PostBuildPlugin;
import aQute.lib.tag.Tag;

public class ReportPostBuildPlugin implements PostBuildPlugin{

	private static final String REPORT_GENERATE = "-report.generate";

	@Override
	public void postBuild(Builder builder) throws Exception {
		Parameters parameters = new Parameters(builder.getProperty(REPORT_GENERATE));
		
		for ( Map.Entry<String, Attrs> e : parameters.entrySet()) {
			String name = e.getKey();
			
			ReportGenerator rg = new ReportGenerator(builder);
			Tag tag = rg.createReport(builder.getJar());
			builder.getJar().putResource( name, new TagResource(tag));
		}		
	}

}
