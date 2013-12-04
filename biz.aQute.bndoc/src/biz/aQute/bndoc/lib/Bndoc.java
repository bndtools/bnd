package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.sed.*;
import aQute.service.reporter.*;

import com.github.rjeschke.txtmark.*;

@Description("")
public abstract class Bndoc implements Closeable, Domain {
	final List<InputStream> inputs = new ArrayList<>();
	final List<InputStream> css = new ArrayList<>();
	final Map<String, String> properties = new HashMap<>();
	PrintWriter out;
	Configuration.Builder config = Configuration.builder();
	BndocDecorator decorator = new BndocDecorator(this);
	String template = "<html>\n" //
			+ "  <head>\n"
			+ "    <title>${title}</title>\n" //
			+ "    ${css}" //
			+ "  </head>\n" //
			+ "  <body>\n" //
			+ "    <div class=bndoc-content>\n"
			+ "      ${content}" //
			+ "    <div>\n"
			+ "    <footer>Copyright ${tstamp;yyyy}</footer>\n" //
			+ "  </body>\n"
			+ "</html>";
	Reporter reporter;
	ReplacerAdapter replacer = new ReplacerAdapter(this);

	{
		config.setDecorator(decorator);
	}

	public Bndoc reporter(Reporter reporter) {
		this.reporter = reporter;
		return this;
	}

	public Bndoc input(File f) throws FileNotFoundException {
		if (f.isFile())
			inputs.add(new FileInputStream(f));
		else if (f.isDirectory()) {
			String[] names = f.list();
			Arrays.sort(names);
			for (String name : names) {
				if (name.endsWith(".md"))
					input(new File(f, name));
			}
		} else
			reporter.error("Input file oes not exist %s", f);
		return this;
	}

	public Bndoc input(Collection<File> files) throws FileNotFoundException {
		for (File f : files) {
			input(f);
		}
		return this;
	}

	public Bndoc input(InputStream in) throws FileNotFoundException {
		inputs.add(in);
		return this;
	}

	public Bndoc input(URL in) throws IOException {
		inputs.add(in.openStream());
		return this;
	}

	public Bndoc style(Collection<File> files) throws FileNotFoundException {
		for (File f : files) {
			style(f);
		}
		return this;
	}

	public Bndoc style(InputStream in) throws FileNotFoundException {
		css.add(in);
		return this;
	}

	public Bndoc style(URL in) throws IOException {
		css.add(in.openStream());
		return this;
	}

	public Bndoc style(File f) throws FileNotFoundException {
		if (f.isFile())
			css.add(new FileInputStream(f));
		else if (f.isDirectory()) {
			String[] names = f.list();
			Arrays.sort(names);
			for (String name : names) {
				if (name.endsWith(".css"))
					input(new File(f, name));
			}
		} else
			reporter.error("Style/css file does not exist %s", f);
		return this;
	}

	public Bndoc template(File template) throws Exception {
		this.template = IO.collect(template);
		return this;
	}

	public Bndoc template(String template) throws Exception {
		this.template = template;
		return this;
	}

	public Bndoc template(InputStream template) throws Exception {
		this.template = IO.collect(template);
		return this;
	}

	public Bndoc property(String key, Object value) {
		properties.put(key, toString(value));
		return this;
	}

	public Bndoc properties(Map<String, String> properties) {
		this.properties.putAll(properties);
		return this;
	}

	private String toString(Object value) {
		if (value instanceof Object[]) {
			return Arrays.toString((Object[]) value);
		}
		return value.toString();
	}

	public Bndoc output(OutputStream out) throws UnsupportedEncodingException {
		this.out = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
		return this;
	}

	public Bndoc output(File out) throws UnsupportedEncodingException, FileNotFoundException {
		return output( new FileOutputStream(out));
	}
	public Bndoc output(PrintWriter out) throws UnsupportedEncodingException {
		this.out = out;
		return this;
	}

	public Bndoc output(Writer out) throws UnsupportedEncodingException {
		this.out = new PrintWriter(out);
		return this;
	}

	public void close() {
		if (out != null)
			out.close();
	}

	@Override
	public Map<String, String> getMap() {
		return properties;
	}

	@Override
	public Domain getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	public abstract void generate() throws Exception;

	public Object _css(String args[]) throws Exception {
		if (css.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder("\n  <style type='text/css'>");
		for (InputStream in : css) {
			try (InputStream i = in) {
				sb.append(IO.collect(i)).append("\n// ----------\n");
			}
		}
		sb.append("\n  </style>\n");
		return sb;
	}

	protected void content() throws IOException {

		Configuration cnf = config.build();
		for (InputStream input : inputs) {
			try (InputStream i = input) {
				String s = IO.collect(i);
				s = replacer.process(s);
				s = Processor.process(s, cnf);
				out.write(s);
			}
		}
	}


}