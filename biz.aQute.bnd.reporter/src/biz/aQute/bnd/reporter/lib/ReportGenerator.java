package biz.aQute.bnd.reporter.lib;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportGeneratorPlugin;
import aQute.lib.tag.Tag;

public class ReportGenerator extends Processor {

	public ReportGenerator(Processor processor) {
		super(processor);
		use(this);
	}

	public Tag createReport(Jar jar) throws Exception {
		Tag top = new Tag("report");
		
		Domain d = Domain.domain(jar.getManifest());

		new Tag(top, "bundle-symbolic-name", d.getBundleSymbolicName().getKey());
		
		for ( ReportGeneratorPlugin rp : getParent().getPlugins(ReportGeneratorPlugin.class))
			top.addContent( rp.report(jar));
		
		return top;
	}

}
