package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;

import aQute.libg.command.*;

public class PD4MLBuilder extends Base {
	private File	output;
	private URI		uri;

	public PD4MLBuilder(DocumentBuilder db, URI uri) {
		super(db);
		this.uri = uri;
	}

	@Override
	public void close() throws IOException {}

	public void convert() throws Exception {
		Command c = new Command();
		c.add("pd4ml");
		c.add(uri.toString());
		c.add(getProperty("pdf.htmlWidth", "1200"));
		c.add(getProperty("pdf.pageFormatName", "A4"));
		c.add("-out");
		c.add(getOutput().getAbsolutePath());
		
		set(c, "author");
		set(c, "bgcolor");
		set(c, "bgimage");
		set(c, "bookmarks");
		set(c, "footer");
		set(c, "header");
		set(c, "insets");
		set(c, "multicolumn");
		set(c, "orientation");
		set(c, "outformat");
		set(c, "pagerange");
		set(c, "password");

		if ( isTrace() ) {
			c.add("-debug");
		}
		
		String ttf = getProperty("pdf.ttf");
		if (ttf != null) {
			File dir = getFile(ttf);
			if (!dir.isDirectory()) {
				error("The PDF TrueType Font directory does not exist %s", dir);
			} else {
				c.add("-ttf");
				c.add(dir.getAbsolutePath());
			}
		}
		int execute = c.execute(System.out, System.err);
		if (execute != 0) {
			error("Executing command %s, error %s", c, execute);
		}
	}

	private void set(Command c, String string) {
		String prop = getProperty("pdf." + string);
		if (prop == null)
			return;

		c.add("-" + string);
		c.add(prop);
	}

	protected File getOutput() {
		if (output == null)
			output = getFile(OUTPUT, false, "single.pdf");
		return output;
	}

}
