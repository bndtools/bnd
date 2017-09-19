package biz.aQute.bnd.reporter.command;

import java.io.File;
import java.net.URL;
import java.util.List;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.tag.Tag;
import aQute.libg.xslt.Transform;
import biz.aQute.bnd.reporter.lib.ReportGenerator;

public class ReporterCommand extends Processor {
	
	private ReporterOptions options;

	public ReporterCommand( Processor p) {
		super(p);
	}

	public interface ReporterOptions extends Options {
		@Description("Output file")
		String output();
	}

	public void run(ReporterOptions options) throws Exception {
		
		this.options = options;
		
		List<String> args = options._arguments();
		if ( args.isEmpty()) {
			error("No command");
			return;
		}
			
		String cmd = args.remove(0);
		options._command().execute(this, cmd, args);
	}
	

	@Description("Generate")
	interface GenerateOptions extends Options {
		String template();
	}
	
	public void _generate(GenerateOptions genOptions) throws Exception {
		System.out.println("Hello Reporter");
		
		for ( String arg : genOptions._arguments()) {
			File f = getFile(arg);
			ReportGenerator rg  = new ReportGenerator(this);

			Jar jar = new Jar(f);
			Tag tag = rg.createReport(jar);

			if ( genOptions.template()!=null) {
				
				URL xslt = getFile(genOptions.template()).toURI().toURL();
				Transform.transform(xslt, tag.toInputStream(), System.out);
			} else
				System.out.println(tag);
		}
	}
	
}
