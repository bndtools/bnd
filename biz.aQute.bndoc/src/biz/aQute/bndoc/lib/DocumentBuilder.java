package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.stathissideris.ascii2image.core.*;
import org.stathissideris.ascii2image.graphics.*;
import org.xml.sax.*;

import aQute.lib.env.*;
import aQute.lib.io.*;

import com.github.rjeschke.txtmark.*;

public class DocumentBuilder extends Base {
	public static final float				QUALITY_SCALE	= 2;
	static Pattern							CONTENT_P		= Pattern.compile("\\$\\{(content|css)\\}");
	static String							ATTRIBUTE_S		= "((.+)\\s*:\\s*(.*)\n)+";
	static Pattern							ATTRIBUTE_P		= Pattern.compile(ATTRIBUTE_S, Pattern.UNIX_LINES);
	static Pattern							ATTRIBUTES_P	= Pattern.compile("(" + ATTRIBUTE_S + ")+",
																	Pattern.UNIX_LINES);

	List<File>								sources			= new ArrayList<>();
	List<File>								css				= new ArrayList<>();
	File									output;
	File									resources;
	String									template;
	String									inner;

	Decorator								decorator;
	ConversionOptions						options;
	File									current;
	HashMap<String,CustomShapeDefinition>	customShapes	= new HashMap<>();

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

	List<TOC>	toc	= new ArrayList<>();

	public DocumentBuilder(Env g) {
		super(g);
	}

	public boolean prepare() throws Exception {
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

		for (final File source : sources) {
			try {
				String content = IO.collect(source);
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
						IO.copy(getClass().getResourceAsStream("resources/style.css"), pw);
					} else {
						for (File f : css) {
							IO.copy(new FileReader(f), pw);
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

	public void single() throws Exception {
		prepare();
		if (output == null)
			output = getFile("single.html");

		output.getParentFile().mkdirs();

		final PrintWriter pw = IO.writer(output);
		setCurrent(output);
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

	// public void multi() throws Exception {
	// prepare();
	// File multi = getOutput(true, "multi");
	// int n = 1000;
	// for (final File source : sources) {
	// File section = IO.getFile(multi, n++ + ".html");
	// setCurrent(section);
	// try (PrintWriter pw = IO.writer(section)) {
	// body(pw, getTemplate(), source);
	// }
	// }
	// }

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

	void body(final PrintWriter pw, String template, final File source) throws MalformedURLException, IOException,
			Exception {

		doTemplate(template, pw, new Runnable() {

			@Override
			public void run() {
				String content;
				try {
					content = IO.collect(source);
					Matcher matcher = ATTRIBUTES_P.matcher(content);
					if (matcher.find()) {
						if (matcher.start() == 0) {
							String header = content.substring(0, matcher.end(0));
							content = content.substring(matcher.end(0));

							matcher = ATTRIBUTE_P.matcher(header);
							while (matcher.find()) {
								String key = matcher.group(1);
								String value = matcher.group(2);
								setProperty(key, value);
							}
						}
					}

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

	public File getCurrent() {
		return current;
	}

	public File setCurrent(File current) {
		this.current = current;
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
}