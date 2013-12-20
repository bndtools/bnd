package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.stathissideris.ascii2image.core.*;
import org.stathissideris.ascii2image.graphics.*;
import org.w3c.dom.*;
import org.xhtmlrenderer.layout.*;
import org.xhtmlrenderer.pdf.*;
import org.xhtmlrenderer.render.*;
import org.xhtmlrenderer.resource.*;
import org.xml.sax.*;

import aQute.bnd.version.*;
import aQute.lib.env.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.tag.*;

import com.github.rjeschke.txtmark.*;
import com.github.rjeschke.txtmark.Configuration.Builder;

public class DocumentBuilder extends Base implements Cloneable {
	static Pattern							DEFINITION_P	= Pattern.compile("(\\[([^]]{2,20})\\])");
	public static final float				QUALITY_SCALE	= 2;
	static Pattern							CONTENT_P		= Pattern.compile("\\$\\{(content|css)\\}");
	static String							ATTRIBUTE_S		= "(?:(.+)\\s*[:=]\\s*([^\n]*)\n)+";
	static Pattern							ATTRIBUTE_P		= Pattern.compile(ATTRIBUTE_S, Pattern.MULTILINE);
	static Pattern							ATTRIBUTES_P	= Pattern.compile("---\\s*\n(" + ATTRIBUTE_S
																	+ ")+---\\s*\n", Pattern.MULTILINE);

	List<File>								sources			= new ArrayList<>();
	List<File>								css				= new ArrayList<>();
	File									output;
	File									resources;
	String									template;
	String									inner;
	float									zoom			= 1.4f;
	Decorator								decorator;
	ConversionOptions						options;
	File									currentOutput;
	File									currentInput;
	HashMap<String,CustomShapeDefinition>	customShapes	= new HashMap<>();
	String									name;
	boolean									clean			= false;

	{
		customShapes.putAll(CustomShapes.shapes);
	}

	class TOC {
		final int		level;
		final int[]		counters;
		final String	title;
		final int		file;

		public TOC(int level, int file, int[] counters, String title) {
			this.level = level;
			this.file = file;
			this.counters = counters;
			this.title = title;
		}
	}

	List<TOC>		toc	= new ArrayList<>();
	private boolean	keep;

	public DocumentBuilder(Env g) {
		super(g);
	}

	public boolean prepare() throws Exception {
		if (!super.prepare()) {
			decorator = new BndocDecorator(this);

			if (getClean()) {
				IO.delete(getOutput());
				if (!getOutput().mkdir())
					error("Could not create output directory %s", getOutput());
			}
			File resources = getResources();
			if (resources != null) {
				for (File sub : resources.listFiles()) {
					IO.copy(sub, new File(getOutput(), sub.getName()));
				}
			}

			toc();
			return false;
		} else
			return true;
	}

	private void toc() throws Exception {
		final AtomicInteger file = new AtomicInteger(1000);
		final AtomicInteger term = new AtomicInteger(1000);
		final ParagraphCounter pgc = new ParagraphCounter();

		DefaultDecorator counter = new DefaultDecorator() {
			int			start	= -1;
			private int	paraStart;

			@Override
			public void openHeadline(StringBuilder out, int level) {
				super.openHeadline(out, level);
				start = out.length();
				pgc.level(level);
			}

			@Override
			public void closeHeadline(StringBuilder out, int level) {
				toc.add(new TOC(level, file.get(), pgc.counters.clone(), out.substring(start + 1)));
				super.closeHeadline(out, level);
			}

			@Override
			public void openParagraph(StringBuilder out) {
				out.append("<p>");
				paraStart = out.length();
			}

			@Override
			public void closeParagraph(StringBuilder out) {
				Matcher m = DocumentBuilder.DEFINITION_P.matcher(out);
				boolean found = m.find(paraStart);
				while (found) {
					String term = m.group(2);
					// TODO handle terms
					found = m.find();
				}
				out.append("</p>\n");
			}
		};

		Configuration conf = getMarkdownConfiguration(counter, null, null);
		for (final File source : sources) {
			try {
				String content = IO.collect(source);
				content = processHeaders(content);
				content = process(content);
				content = Processor.process(content, conf);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			file.incrementAndGet();
		}

	}

	Configuration getMarkdownConfiguration(Decorator decorator, BlockEmitter block, SpanEmitter span) {
		Builder builder = Configuration.builder();
		builder.enableSafeMode();
		builder.forceExtentedProfile();
		if (decorator != null)
			builder.setDecorator(decorator);
		if (block != null)
			builder.setCodeBlockEmitter(block);
		if (span != null)
			builder.setSpecialLinkEmitter(span);

		return builder.build();
	}

	public String _toc(String[] args) throws IOException {
		int level = 3;
		if (args.length > 1)
			level = Integer.parseInt(args[1]);

		Tag table = new Tag("table").addAttribute("class", "toc").addAttribute("id", "_toc");
		Tag colgroup = new Tag(table, "colgroup");
		new Tag(colgroup, "col").addAttribute("class", "number");
		new Tag(colgroup, "col").addAttribute("class", "title");
		new Tag(colgroup, "col").addAttribute("class", "page");
		Tag thead = new Tag(table, "thead");
		Tag theadrow = new Tag(thead, "tr");
		new Tag(theadrow, "th").addAttribute("class", "number");
		new Tag(theadrow, "th").addAttribute("class", "title");
		new Tag(theadrow, "th").addAttribute("class", "page");
		Tag tbody = new Tag(table, "tbody");
		for (TOC entry : toc) {
			if (entry.level > level)
				continue;
			String number = ParagraphCounter.toString(entry.level, ".", entry.counters);
			String pageref = "_h-" + number;

			Tag tbodyrow = new Tag(tbody, "tr").addAttribute("class", "h" + entry.level);
			new Tag(tbodyrow, "td").addAttribute("class", "number").addContent(number);
			Tag title = new Tag(tbodyrow, "td").addAttribute("class", "title");
			new Tag(title, "a").addAttribute("href", "#" + pageref).addContent(entry.title);
			new Tag(tbodyrow, "td").addAttribute("class", "page").addAttribute("pageref", pageref);
		}

		StringBuilder sb = new StringBuilder();
		append(sb,table);
		return sb.toString();
	}

	void doTemplate(String template, PrintWriter pw, Runnable r) throws IOException {
		Matcher m = CONTENT_P.matcher(template);
		int start = 0;
		while (m.find()) {
			String prefix = template.substring(start, m.start());
			prefix = process(prefix);
			pw.write(prefix);
			start = m.end();

			switch (m.group(1)) {
				case "content" :
					r.run();
					break;
				case "css" :
					pw.println("<style>");
					if (css.isEmpty()) {
						preprocess(IO.reader(getClass().getResourceAsStream("resources/style.css")), pw);
					} else {
						for (File f : css) {
							preprocess(IO.reader(f), pw);
						}
					}
					pw.println("</style>");
					break;
			}
		}
		String suffix = template.substring(start);
		suffix = process(suffix);
		pw.write(suffix);
	}

	private void preprocess(BufferedReader reader, PrintWriter pw) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			line = process(line);
			pw.write(line);
		}
	}

	public void single() throws Exception {
		prepare();
		File out = new File(getOutput(), getName("html"));
		single(out);
	}

	private void single(File out) throws IOException {
		final PrintWriter pw = IO.writer(out);
		setCurrentOutput(out);

		doTemplate(getTemplate(), pw, new Runnable() {

			@Override
			public void run() {
				try {
					for (final File source : sources) {
						body(pw, getInnerTemplate(), source);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
		pw.close();
	}

	public void pdf() throws Exception {
		prepare();

		File tmp = IO.createTempFile(getOutput(), "bndoc", ".html");
		try {
			single(tmp);

			if (isOk()) {
				File pdf = new File(getOutput(), getName("pdf"));
				pdf(tmp, pdf);
			}
		}
		finally {
			if (!keep)
				IO.delete(tmp);
		}
	}

	public void pdf(File from, File to) throws Exception {
		String url = from.toURI().toString();
		ITextRenderer renderer = new ITextRenderer(getZoom() * 1.0f * 20f * 4f / 3f, 20);
		ResourceLoaderUserAgent callback = new ResourceLoaderUserAgent(renderer.getOutputDevice());
		callback.setSharedContext(renderer.getSharedContext());
		renderer.getSharedContext().setUserAgentCallback(callback);
		Document doc = XMLResource.load(new InputSource(url)).getDocument();

		renderer.setDocument(doc, url);
		renderer.layout();

		//
		// We now must fill in page numbers for our Table of Content
		//

		LayoutContext layoutContext = renderer.getSharedContext().newLayoutContextInstance();
		BlockBox root = renderer.getRootBox();
		Map<String,BlockBox> idMap = renderer.getSharedContext().getIdMap();

		Document document = renderer.getDocument();
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList list = (NodeList) xpath.evaluate("//*[@pageref]", document, XPathConstants.NODESET);
		if (list != null) {
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				Node item = node.getAttributes().getNamedItem("pageref");
				String ref = item.getNodeValue();
				BlockBox box = idMap.get(ref);
				if (box != null) {
					PageBox pp = root.getLayer().getLastPage(layoutContext, box);
					int pageno = pp.getPageNo();
					Text text = document.createTextNode(Integer.toString(pageno));
					node.appendChild(text);
				} else
					warning("page reference to %s not found", item.getNodeValue());
			}
		}
		//
		// Re-relayout to see the content
		//
		renderer.layout();

		try (FileOutputStream fout = new FileOutputStream(to)) {
			renderer.createPDF(fout);
		}
		catch (Exception e) {
			error("failed to created PDF %s", e);
		}
	}

	private static class ResourceLoaderUserAgent extends ITextUserAgent {
		public ResourceLoaderUserAgent(ITextOutputDevice outputDevice) {
			super(outputDevice);
		}

		protected InputStream resolveAndOpenStream(String uri) {
			InputStream is = super.resolveAndOpenStream(uri);
			return is;
		}
	}

	void body(final PrintWriter pw, String template, final File source) throws MalformedURLException, IOException,
			Exception {

		doTemplate(template, pw, new Runnable() {

			@Override
			public void run() {
				Configuration cnf = getMarkdownConfiguration(decorator, null, null);
				String content;
				try {
					setCurrentInput(source);
					content = IO.collect(source);
					content = processHeaders(content);
					String doctitle = getProperty("doctitle");
					if (doctitle != null)
						pw.printf("<div class='print doctitle'>%s</div>", doctitle);

					content = process(content);
					content = Processor.process(content, decorator);
					pw.write(content);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
	}

	String processHeaders(String content) {
		Matcher matcher = ATTRIBUTES_P.matcher(content);
		if (matcher.find()) {
			if (matcher.start() == 0) {
				String headers[] = content.substring(matcher.start(1), matcher.end(1)).split("\n");
				content = content.substring(matcher.end(0));

				for (String header : headers) {
					String[] kv = header.split("\\s*[:=]\\s*", 2);
					setProperty(kv[0], kv[1]);
				}
			}
		}
		return content;
	}

	public String getTemplate() throws IOException {
		if (template == null) {
			template = IO.collect(getClass().getResourceAsStream("resources/outer.htm"));
		}

		return template;
	}

	public String getInnerTemplate() throws IOException {
		if (inner == null)
			inner = IO.collect(getClass().getResourceAsStream("resources/inner.htm"));

		return inner;
	}

	@Override
	public void close() throws IOException {}

	public ConversionOptions getConversionOptions() throws ParserConfigurationException, SAXException, IOException {
		if (options == null) {
			options = new ConversionOptions();
			options.processingOptions = new ProcessingOptions();
			options.renderingOptions = new RenderingOptions();
			options.renderingOptions.setScale(QUALITY_SCALE);
			options.renderingOptions.setAntialias(true);
			ConfigurationParser cp = new ConfigurationParser();

			HashMap<String,CustomShapeDefinition> shapes = cp.getShapeDefinitionsHash();
			shapes.putAll(customShapes);
			options.processingOptions.setCustomShapes(shapes);
		}
		return options;
	}

	public File getResources() {
		return resources;
	}

	public File getCurrentOutput() {
		return currentOutput;
	}

	public File setCurrentOutput(File current) {
		this.currentOutput = current;
		return current;
	}

	public File getCurrentInput() {
		return currentInput;
	}

	public File setCurrentInput(File current) {
		this.currentInput = current;
		return current;
	}

	public void setResources(File resources) {
		this.resources = resources;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public void setInner(String inner) {
		this.inner = inner;
	}

	public void addSource(File f) {
		sources.add(f);
	}

	public void setOutput(File file) {
		output = file;
	}

	public File getOutput() {
		if (output == null) {
			output = getFile(getProperty("output", "www"));
			output.mkdirs();
			if (!output.isDirectory()) {
				error("Cannot create the output directory %s", output);
			}
		}
		return output;
	}

	public void addSources(List<File> sources) {
		this.sources.addAll(sources);
	}

	public void addCustomShape(String name, Map<String,String> properties) {
		CustomShapes shape = new CustomShapes(name, properties);
		if (shape.getError() != null)
			error("Invalid shape def %s : %s", name, shape.getError());
		customShapes.put(name, shape);

	}

	public void addCSS(File f) {
		css.add(f);
	}

	public void setZoom(float zoom) {
		this.zoom = zoom;
	}

	public float getZoom() {
		if (zoom == 0) {
			String z = getProperty("zoom");
			if (z == null) {
				zoom = 1.0f;
			} else
				zoom = Float.parseFloat(z);
		}
		return zoom;
	}

	public String getName(String extension) {
		if (name == null) {
			name = "index" + "." + extension;
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean getClean() {
		return clean || isTrue(getProperty("clean"));
	}

	public void setClean(boolean b) {
		this.clean = b;
	}

	/**
	 * Create a resource in the output directory of a given type from path. The
	 * path can be relative from the currentOutput output file (
	 * {@link #getCurrentOutput()} or absolute. This resource is then copied to
	 * the output {@code type} directory. It then returns the path from the
	 * output directory.
	 * 
	 * @param type
	 *            the type of resource
	 * @param path
	 *            the path/uri to the resource
	 * @return the relative name from the output directory
	 */
	String toResource(String type, String path) {
		try {
			long lastModified = 0;

			URI uri = new URI(path);
			if (!uri.isAbsolute()) {
				uri = getCurrentInput().toURI().resolve(path);
				File source = IO.getFile(getCurrentInput().getParentFile(), path);
				if (!source.isFile()) {
					error("Missing %s reference, file %s does not exist ", type, source);
					return path;
				}
				lastModified = source.lastModified();
			}
			String extension = "";
			int n = path.lastIndexOf('.');
			if (n >= 0) {
				extension = path.substring(n);
			}
			String name = type + "/" + identity(uri.toString()) + extension;

			File f = IO.getFile(getOutput(), name);
			f.getParentFile().mkdirs();
			if (!f.isFile() || f.lastModified() < lastModified) {
				IO.copy(uri.toURL(), f);
			}
			return name;
		}
		catch (Exception e) {
			error("Failed to create resource of type %s, %s: %s", type, path, e);
			return null;
		}
	}

	String identity(String string) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			for (int i = 0; i < string.length(); i++) {
				char c = string.charAt(i);
				md.update((byte) c);
				md.update((byte) (c >> 8));
			}
			return Hex.toHexString(md.digest());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setKeep(boolean b) {
		keep = b;
	}

	/**
	 * Modify a version to set a version policy. Thed policy is a mask that is
	 * mapped to a version.
	 * 
	 * <pre>
	 * +           increment
	 * -           decrement
	 * =           maintain
	 * &tilde;           discard
	 * 
	 * ==+      = maintain major, minor, increment micro, discard qualifier
	 * &tilde;&tilde;&tilde;=     = just get the qualifier
	 * version=&quot;[${version;==;${@}},${version;=+;${@}})&quot;
	 * </pre>
	 * 
	 * @param args
	 * @return
	 */
	final static String		MASK_STRING		= "[\\-+=~0123456789]{0,3}[=~]?";
	final static Pattern	MASK			= Pattern.compile(MASK_STRING);
	final static String		_versionHelp	= "${version;<mask>;<version>}, modify a version\n"
													+ "<mask> ::= [ M [ M [ M [ MQ ]]]\n" + "M ::= '+' | '-' | MQ\n"
													+ "MQ ::= '~' | '='";

	public String _version(String args[]) {
		String mask = args[1];

		Version version = null;
		if (args.length >= 3)
			version = new Version(args[2]);

		return version(version, mask);
	}

	String version(Version version, String mask) {
		StringBuilder sb = new StringBuilder();
		String del = "";

		for (int i = 0; i < mask.length(); i++) {
			char c = mask.charAt(i);
			String result = null;
			if (c != '~') {
				if (i == 3) {
					result = version.getQualifier();
				} else if (Character.isDigit(c)) {
					// Handle masks like +00, =+0
					result = String.valueOf(c);
				} else {
					int x = version.get(i);
					switch (c) {
						case '+' :
							x++;
							break;
						case '-' :
							x--;
							break;
						case '=' :
							break;
					}
					result = Integer.toString(x);
				}
				if (result != null) {
					sb.append(del);
					del = ".";
					sb.append(result);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * A macro to cut out a block from an output stream. It allows you to specify
	 * the number of rows, the number of columns, and the shift it should do on the left
	 * with spaces.
	 */
	public String _block(String[] args) {
		if (args.length < 4)
			return "";

		int width = Integer.parseInt(args[1]);
		int height = Integer.parseInt(args[2]);
		int shift = Integer.parseInt(args[3]);

		StringBuilder sb = new StringBuilder();
		
		// 
		// If an output has a ;, we concatenate it
		//
		String del = "";
		for (int i = 4; i < args.length; i++) {
			sb.append(del).append(args[i]);
			del = ";";
		}
		String indent = "                                                                      ";
		if ( shift < indent.length())
			indent = indent.substring(0, shift);

		int rover = 0;
		for (int row = 0; row < height; row++) {
			if (row != 0) {
				sb.insert(rover, indent);
				rover += indent.length();
			}
			int col = 0;
			row: while (rover < sb.length()) {
				char c = sb.charAt(rover);
				switch (c) {
					case '\n' :
						rover++;
						break row;
					default :
						col++;
						if (col > width) {
							sb.delete(rover, rover+1);
						} else
							rover++;
						break;
				}
			}
		}
		if ( rover != sb.length()) {
			sb.delete(rover, sb.length());
			sb.append(indent).append("...\n");
		}
		return sb.toString();
	}

	/**
	 * Helper method to output a tag without escaping its contents, since in our
	 * case we already have HTML in the contents.
	 * 
	 * @param out
	 * @param tag
	 * @throws IOException
	 */
	public static void append(Appendable out, Tag tag) throws IOException {
		out.append('<');
		out.append(tag.getName());

		for (Entry<String,String> entry : tag.getAttributes().entrySet()) {
			String value = Tag.escape(entry.getValue());
			out.append(' ');
			out.append(entry.getKey());
			out.append("=");
			String quote = "'";
			if (value.indexOf(quote) >= 0)
				quote = "\"";
			out.append(quote);
			out.append(value);
			out.append(quote);
		}
		out.append('>');

		for (Object content : tag.getContents()) {
			if (content instanceof Tag) {
				append(out, (Tag) content);
			} else {
				out.append(content.toString());
			}
		}
		out.append("</");
		out.append(tag.getName());
		out.append(">\n");
	}
}