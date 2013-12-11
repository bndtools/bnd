package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.stathissideris.ascii2image.core.*;
import org.stathissideris.ascii2image.graphics.*;
import org.xml.sax.*;

import aQute.lib.env.*;
import aQute.lib.io.*;

import com.github.rjeschke.txtmark.*;

class DocumentBuilder extends Base {
	public static final float	QUALITY_SCALE	= 2;
	static Pattern	CONTENT_P	= Pattern.compile("\\$\\{content\\}");

	enum Format {
		PDF, SINGLE, MULTI
	};

	Map<URI,Props>		sources;
	File				images;
	File				output;
	private File				current;
	String				template;
	String				innerTemplate;
	Decorator			decorator;
	ConversionOptions	options;
	List<File>			shapes;

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

	List<TOC>	toc	= new ArrayList<>();

	DocumentBuilder(Base g) {
		super(g);
	}

	protected boolean prepare() throws Exception {
		if (!super.prepare()) {
			decorator = new BndocDecorator(this);
			
			toc();
			return false;
		} else
			return true;
	}

	private void toc() throws Exception {
		final AtomicInteger file = new AtomicInteger(1000);
		final ParagraphCounter pgc = new ParagraphCounter();

		DefaultDecorator counter = new DefaultDecorator() {
			int	start	= -1;

			@Override
			public void openHeadline(StringBuilder out, int level) {
				super.openHeadline(out, level);
				start = out.length();
				pgc.level(level);
			}

			@Override
			public void closeHeadline(StringBuilder out, int level) {
				toc.add(new TOC(level, file.get(), pgc.counters.clone(), out.substring(start)));
				super.closeHeadline(out, level);
			}
		};

		for (final Map.Entry<URI,Props> entry : getSources().entrySet()) {
			try {
				String content = getContent(entry.getKey());
				content = process(content);
				content = Processor.process(content, counter);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			file.incrementAndGet();
		}

	}

	public String _toc(String[] args) {
		int level = 2;
		if (args.length > 1)
			level = Integer.parseInt(args[1]);

		try (Formatter f = new Formatter()) {
			f.format("<div class=bndoc-toc>\n");
			for (TOC entry : toc) {
				if (entry.level >= level)
					continue;
				f.format("<div class=bndoc-toc-h%s>%s%s</div>\n", entry.level + 1,
						ParagraphCounter.toHtml(entry.level, ".", entry.counters), entry.title);
			}
			f.format("</div>\n");
			return f.toString();
		}
	}

	void doTemplate(String template, PrintWriter pw, Runnable r) {
		Matcher m = CONTENT_P.matcher(template);
		int start = 0;
		while (m.find()) {
			String prefix = template.substring(start, m.start());
			prefix = process(prefix);
			pw.write(prefix);
			start = m.end();
			r.run();
		}
		String suffix = template.substring(start);
		suffix = process(suffix);
		pw.write(suffix);
	}

	public void single() throws Exception {
		prepare();
		final PrintWriter pw = IO.writer(setCurrent(getOutput(false, "single.html")));

		doTemplate(getTemplate(), pw, new Runnable() {

			@Override
			public void run() {
				try {
					for (final Map.Entry<URI,Props> entry : getSources().entrySet()) {
						body(pw, getInnerTemplate(), entry.getKey(), entry.getValue());
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
		pw.close();
	}

	public void multi() throws Exception {
		prepare();
		File multi = getOutput(true, "multi");
		int n = 1000;
		for (final Map.Entry<URI,Props> entry : getSources().entrySet()) {
			File section = IO.getFile(multi, n++ + ".html");
			setCurrent(section);
			try (PrintWriter pw = IO.writer(section)) {
				body(pw, getTemplate(), entry.getKey(), entry.getValue());
			}
		}
	}

	public void pdf() throws Exception {
		prepare();
		try (DocumentBuilder tmp = new DocumentBuilder(this)) {
			File out = IO.createTempFile(null, "bndoc", ".html");
			File img = new File(out.getAbsolutePath() + "-imgs");
			img.mkdirs();
			tmp.setProperty(OUTPUT, out.getAbsolutePath());
			tmp.setProperty(IMAGES, img.getAbsolutePath());
			tmp.single();
			getInfo(tmp);
			if (isOk()) {
				PD4MLBuilder p = new PD4MLBuilder(this, out.toURI());
				p.convert();
				getInfo(p);
				p.close();
			}
		}
	}

	void body(final PrintWriter pw, String template, final URI uri, Props props) throws MalformedURLException,
			IOException, Exception {

		doTemplate(template, pw, new Runnable() {

			@Override
			public void run() {
				String content;
				try {
					content = getContent(uri);
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

	public String getTemplate() throws IOException {
		if (template == null)
			template = getTemplate(TEMPLATE, DEFAULT_TEMPLATE);

		return template;
	}

	public String getInnerTemplate() throws IOException {
		if (innerTemplate == null)
			innerTemplate = getTemplate(INNER_TEMPLATE, DEFAULT_INNER_TEMPLATE);

		return innerTemplate;
	}

	private String getTemplate(String name, String defaultContent) throws IOException {
		String path = getProperty(name);
		if (path == null)
			return defaultContent;
		else {
			File f = IO.getFile(path);
			if (!f.isFile()) {
				error("Template %s not found at %s. Using default.", name, path);
				return defaultContent;
			} else {
				return IO.collect(f);
			}
		}
	}

	public Map<URI,Props> getSources() throws Exception {
		if (sources == null)
			sources = toURis(SOURCES);

		return sources;
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

			for (File entry : getShapeFiles()) {
				cp.parseFile(entry);
			}
			HashMap<String,CustomShapeDefinition> defs = cp.getShapeDefinitionsHash();
			
			for ( Entry<String,Props> entry : getHeader(SYMBOLS).entrySet()) {
				defs.put(entry.getKey(), new CustomShapes(entry.getKey(), entry.getValue()));
			}

			defs.putAll(CustomShapes.shapes);
			options.processingOptions.setCustomShapes(cp.getShapeDefinitionsHash());
		}
		return options;
	}

	public List<File> getShapeFiles() {
		if (shapes == null) {
			shapes = new ArrayList<>();
			for (String entry : getHeader(SHAPES).keySet()) {
				File f = getFile(entry);
				if (!f.isFile()) {
					error("No such shape file %s", f);
				} else
					shapes.add(f);

			}
		}
		return shapes;
	}

	protected File getOutput(boolean dir, String defaultName) {
		if (output == null)
			output = getFile(OUTPUT, dir, defaultName);
		return output;
	}

	public void generate() throws Exception {
		Format type = getType();
		if (type == null)
			return;

		switch (type) {
			case MULTI :
				multi();
				break;
			case PDF :
				pdf();
				break;
			case SINGLE :
				single();
				break;
		}
	}

	protected Format getType() {
		String type = getProperty(TYPE, "SINGLE");
		try {
			return Format.valueOf(type);
		}
		catch (Exception e) {
			error("Invalid format specification %s, must be one of %s", type, Format.values());
			return null;
		}
	}

	public File getImages() {
		if (images == null) {
			images = getFile(getProperty(IMAGES, "img"));
		}
		return images;
	}

	public File getCurrent() {
		return current;
	}

	public File setCurrent(File current) {
		this.current = current;
		return current;
	}
}