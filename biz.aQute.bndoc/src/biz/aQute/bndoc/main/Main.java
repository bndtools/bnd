package biz.aQute.bndoc.main;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import aQute.lib.consoleapp.*;
import aQute.lib.env.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.glob.*;
import biz.aQute.bndoc.lib.*;

/**
 * This is the command line interface for a {@link DocumentBuilder}.
 */
@Description("A simple single and multi HTML generator as well as PDF.")
public class Main extends AbstractConsoleApp {

	public Main() throws Exception {
		super();
	}

	@Description("The main options for bndoc")
	interface BndocOptions extends MainOptions {}

	/**
	 * Local initialization point. Is called first for the global commands.
	 */

	public void __main(BndocOptions opts) throws IOException {
		super.__main(opts);
	}

	interface GenerateOptions extends Options {

		@Description("Clean the output directories")
		boolean clean();

		@Description("Add additional properties from files.")
		List<String> properties();

		@Description("The directory where to output the resources")
		String output();

		@Description("The file name of the primary file generated")
		String name();

		@Description("Load SVG diagrams for DITAA")
		List<String> diagrams();

		@Description("Specify a directory with images, css, etc. All files and directories from the given directory are copied to the output directory.")
		String resources();
	}

	@Arguments(arg="sources...")
	@Description("Provide the options to generate a single HTML file.")
	interface HtmlOptions extends GenerateOptions {
		@Description("The main outer template. This normally contains the <html> tag. The template "
				+ "should have a ${content} macro for the contents. If not provided, a default will "
				+ "be used.")
		String template();

		@Description("The inner template applied around each markdown file. This is normally "
				+ "<section>${contents}</section>, which will be provided as a default.")
		String inner();

		@Description("Specify css files. These CSS files are copied into the template when the ${css} macro is used")
		List<String> css();
	}

	static Pattern	WILDCARD_P	= Pattern.compile("([^*]*)(\\*)?\\*\\.(\\w+)$");

	@Description("Generate a single html file")
	public void _html(HtmlOptions options) throws Exception {
		DocumentBuilder db = getHtmlDocumentBuilder(options);

		if (isOk() && db.isOk()) {
			db.single();

			if (options.output() == null)
				IO.copy(db.getOutput(), System.out);
		}
		getInfo(db);
	}

	@Arguments(arg="sources...")
	interface PDFOptions extends HtmlOptions {
		@Description("Specify the page size, default is A4")
		PageSize size();

		@Description("Specify an overall zoom factor. Default is 1.0")
		float zoom();

		@Description("Keep the intermediate HTML file")
		boolean keep();
	}

	@Description("Generate a pdf file")
	public void _pdf(PDFOptions options) throws Exception {
		DocumentBuilder db = getPdfDocumentBuilder(options);

		db.prepare();

		if (isOk() && db.isOk()) {
			db.pdf();

			if (options.output() == null)
				IO.copy(db.getOutput(), System.out);
		}
		getInfo(db);
	}

	@Arguments(arg = "file")
	interface RenderOptions extends PDFOptions {}

	@Description("Convert an HTML file to a PDF")
	public void _render(RenderOptions options) throws Exception {

		File from = getFile(options._().get(0), "Should exist %s");
		if (from != null && from.isFile()) {

			DocumentBuilder db = getPdfDocumentBuilder(options);

			File to;
			if (options.output() == null) {
				to = getFile(from.getAbsolutePath().replace("\\.html?$", ".pdf"));
			} else {
				to = getFile(options.output());
			}
			to.getParentFile().mkdirs();
			db.setOutput(to);

			db.pdf(from, to);
			getInfo(db);
		} else
			error("No such file %s", from);
	}

	/**
	 * Parse the PDF (and thus HTML options).
	 * @param options
	 * @return
	 */
	private DocumentBuilder getPdfDocumentBuilder(PDFOptions options) throws Exception {

		DocumentBuilder db = getHtmlDocumentBuilder(options);
		if (options.size() != null)
			db.setProperty("page-size", options.size().toString());

		if (options.zoom() != 0) {
			db.setZoom(options.zoom());
		}

		if (options.keep())
			db.setKeep(true);

		return db;
	}

	/**
	 * Parse the HTML options
	 * @param options
	 * @return
	 */
	private DocumentBuilder getHtmlDocumentBuilder(HtmlOptions options) throws Exception {
		DocumentBuilder db = new DocumentBuilder(this);

		File resources = getFile(options.resources() == null ? "www" : options.resources());
		resources.mkdirs();
		if (!resources.isDirectory())
			error("No such directory (nor can it be made) %s", resources);

		File template = options.template() != null ? getFile(options.template(), "Template") : null;
		File inner = options.inner() != null ? getFile(options.inner(), "Inner template") : null;

		db.setResources(resources);

		if (template != null)
			db.setTemplate(IO.collect(template));
		else
			db.setTemplate(null);

		if (inner != null)
			db.setInner(IO.collect(inner));
		else
			db.setInner(null);

		if (options.output() != null) {
			db.setOutput(getFile(options.output()));

		} else {
			File tmp = File.createTempFile("bndoc", ".html");
			db.setOutput(tmp);
		}

		if (options.css() != null) {
			List<File> expand = expand(options.css());
			for (File f : expand) {
				db.addCSS(f);
			}
		}

		if (options.name() != null) {
			db.setName(options.name());
		}
		List<File> propertyFiles = expand(options.properties());
		for (File f : propertyFiles) {
			setProperties(f);
		}

		Header h = new Header(getProperty("symbols"));
		for (Entry<String,Props> entry : h.entrySet()) {
			db.addCustomShape(entry.getKey(), entry.getValue());
		}

		db.addSources(expand(options._()));

		if (options.clean()) {
			db.setClean(true);
		}

		return db;
	}

	private List<File> expand(List<String> list) {
		List<File> result = new ArrayList<>();
		if (list == null)
			return result;

		for (String source : list) {
			Matcher m = WILDCARD_P.matcher(source);
			boolean recursive = false;
			Glob glob = null;
			if (m.matches()) {
				source = m.group(1);
				if (source.isEmpty())
					source = ".";
				recursive = m.group(2) != null;
				glob = new Glob(m.group(3));
			}

			File f = getFile(source);
			if (f != null) {
				if (f.isDirectory()) {
					traverse(result, f, glob, recursive);
				} else if (f.isFile()) {
					result.add(f);
				}
			}
		}
		return result;
	}

	private void traverse(List<File> list, File f, Glob glob, boolean recursive) {
		for (File sub : f.listFiles()) {
			if (sub.isFile()) {
				if (glob == null || glob.matcher(sub.getName()).matches())
					list.add(sub);
			} else {
				if (recursive)
					traverse(list, sub, glob, recursive);
			}
		}

	}

	@Description("Show the version of this bndoc")
	public void _version(Options opts) throws IOException {
		System.out.println( IO.collect(getClass().getResourceAsStream("/version.txt")));
	}
	
	/**
	 * Show the credits
	 */
	@Description("Show the credits for use open source programs")
	public void _credits(Options opts) {
		out.printf("Name           Description              Primary Author    License   URL%n");
		out.printf("DITAA          Ascii Arto to png        Stathis Sideris   LGPL-3    http://ditaa.sourceforge.net/%n");
		out.printf("Txtmark        Java markdown processor  René Jeschke      ASL-2     https://github.com/rjeschke/txtmark%n");
		out.printf("Flying Saucer  HTML/CSS 2.1 renderer                      LGPL-3    https://code.google.com/p/flying-saucer/%n");
		out.printf("iText 2.1.7    PDF Generator                              MPL       http://itextpdf.com//%n");
	}

	public static void main(String args[]) throws Exception {
		Main main = new Main();
		main.run(args);
	}
}
