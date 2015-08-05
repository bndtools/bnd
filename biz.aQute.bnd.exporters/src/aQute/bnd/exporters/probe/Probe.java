package aQute.bnd.exporters.probe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.lib.io.IO;

/**
 * Collects all capabilities of this environment and stores them in a file in
 * the home directory.
 *
 */
public class Probe extends Thread implements BundleActivator {
	static final String PROBE_QUERY = "probe.query";


	private BundleContext context;
	private Parameters packages = new Parameters();
	private Parameters capabilities = new Parameters();
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		start();
	}

	public void run() {
		try {
			String filename = context.getProperty(PROBE_QUERY);

			if (filename == null) {
				filename = "~/resource.mf";
			}

			File file = IO.getFile(filename);

			System.out.println("Reading "+file.getAbsolutePath());
			

			doPackages(context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES));
			doPackages(context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA));
			doCapabilities(context.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES));
			doCapabilities(context.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA));

			for (Bundle b : context.getBundles()) {
				doPackages(b.getHeaders().get(Constants.EXPORT_PACKAGE));
				doCapabilities(b.getHeaders().get(Constants.PROVIDE_CAPABILITY));
			}

			FileOutputStream fout = new FileOutputStream(file);
			OutputStream out;
			if ( file.getName().endsWith(".gz"))
				out =new GZIPOutputStream(fout,Deflater.BEST_COMPRESSION);
			else
				out = fout;

			PrintStream ps = new PrintStream(out);
			
			ps.printf("Manifest-Version: 1.0\r\n");
			print(ps, "Export-Package: \r\n ", packages);
			print(ps, "Provide-Capability: \r\n ", capabilities);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void print(PrintStream ps, String del, Parameters parameters) throws IOException {
		for ( Entry<String, Attrs> e : parameters.entrySet()) {
			ps.print(del);
			String name = e.getKey();
			while( name.endsWith("~")) {
				name=name.substring(0,name.length()-1);
			}
			ps.print(name);
			
			Attrs attrs = e.getValue();
			for ( Entry<String, String> ea : attrs.entrySet()) {
				StringBuilder sb = new StringBuilder();
				attrs.append(sb, ea);
				int offset = 0;
				while ( sb.length() - offset > 72 ) {
					offset += 72;
					sb.insert(offset, "\r\n ");
				}
				ps.print(";\r\n  ");
				ps.print(sb.toString());
			}

			del = ",\r\n ";
		}
		ps.printf("\r\n");
	}

	private void doCapabilities(String caps) {
		if ( caps == null || caps.isEmpty())
			return;
		
		Parameters pa = new Parameters(caps);
		capabilities.putAll(pa);
	}

	private void doPackages(String exports) {
		if ( exports == null || exports.isEmpty())
			return;
		
		Parameters pa = new Parameters(exports);
		packages.putAll(pa);
	}
	


	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
