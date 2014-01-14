package biz.aQute.bndoc.main;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.consoleapp.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.glob.*;
import biz.aQute.bndoc.lib.*;
import biz.aQute.markdown.*;
import biz.aQute.markdown.ditaa.*;

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
		List<String> resources();
	}

	@Arguments(arg = "sources...")
	@Description("Provide the options to generate a single HTML file.")
	interface HtmlOptions extends GenerateOptions {
		@Description("The main outer template. This normally contains the <html> tag. The template "
				+ "should have a ${content} macro for the contents. If not provided, a default will " + "be used.")
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
		File output = getFile(options.output());

		if ( options.clean())
			IO.delete(output);
		
		output.mkdirs();
		
		Markdown md = new Markdown(this);
		md.addHandler(new AsciiArtHandler());

		if ( options.properties() != null) {
			List<File> files = expand(options.properties());
			for ( File file: files) {
				md.addProperties(file, null);
			}
		}
		
		Configuration configuration = md.getConfiguration();
		configuration.resources_dir(new File(output, "www"));
		configuration.resources_relative("www/");

		if (options.css() != null) {
			for (String css : options.css()) {
				String style = collect(css, null);
				if (style != null)
					md.addStyle(style);
				else
					error("Indicated style not found %s", css);
			}
		} else {
			String style = collect(null, "style.css");
			md.addStyle(style);
		}

		String outer = collect(options.template(), "outer.htm");
		String inner = collect(options.template(), "inner.htm");

		List<File> files = expand(options._());
		for (File f : files)
			md.parse(f);

		if ( options.resources() != null) {
			for ( String resourceDir : options.resources()) {
				File f = getFile(resourceDir);
				if ( f.isFile()) {
					IO.copy(f, IO.getFile(output, f.getName()));
				} if ( f.isDirectory()) {
					IO.copy(f, output);
				} else {
					error("No such resource %s", f);
				}
			}
		}
		

		if (md.isOk()) {
			File out = new File(output, options.name() == null ? "index.html" : options.name());
			Writer w = IO.writer(out);
			md.single(w, outer, inner);
			w.close();
		}
		getInfo(md);
	}

	private String collect(String fileOrUrl, String backup) throws IOException {
		if (fileOrUrl != null) {
			File f = IO.getFile(fileOrUrl);
			if (f.isFile())
				return IO.collect(f);
			
			try {
				URL url = new URL(fileOrUrl);
				return IO.collect(url.openStream());
			} catch( Exception e) {
				// ignore
			}
		}
		
		InputStream rin = Main.class.getResourceAsStream("resources/"+backup);
		if ( rin != null)
			return IO.collect(rin);

		return null;
	}

	@Arguments(arg = "sources...")
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
	 * 
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
	 * 
	 * @param options
	 * @return
	 */
	private DocumentBuilder getHtmlDocumentBuilder(HtmlOptions options) throws Exception {
		DocumentBuilder db = new DocumentBuilder(this);

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
		File[] listFiles = f.listFiles();
		Arrays.sort(listFiles);

		for (File sub : listFiles) {
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
		System.out.println(IO.collect(getClass().getResourceAsStream("/version.txt")));
	}

	/**
	 * Show the credits
	 */
	@Description("Show the credits for use open source programs")
	public void _credits(Options opts) {
		out.printf("Name           Description              Primary Author    License   URL%n");
		out.printf("-----------------------------------------------------------------------%n");
		out.printf("DITAA          Ascii Arto to png        Stathis Sideris   LGPL-3    http://ditaa.sourceforge.net/%n");
		out.printf("Flying Saucer  HTML/CSS 2.1 renderer                      LGPL-3    https://code.google.com/p/flying-saucer/%n");
		out.printf("iText 2.1.7    PDF Generator                              MPL       http://itextpdf.com//%n");
	}

	public static void main(String args[]) throws Exception {
		Main main = new Main();
		main.run(args);
	}

}
