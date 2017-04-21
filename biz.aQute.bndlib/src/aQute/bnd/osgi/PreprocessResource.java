package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import aQute.lib.io.IO;

public class PreprocessResource extends AbstractResource {
	final Resource	resource;
	final Processor	processor;

	public PreprocessResource(Processor processor, Resource r) {
		super(r.lastModified());
		this.processor = processor;
		this.resource = r;
		setExtra(resource.getExtra());
	}

	@Override
	protected byte[] getBytes() throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(2000);
		PrintWriter pw = IO.writer(bout, Constants.DEFAULT_CHARSET);
		try (BufferedReader rdr = IO.reader(resource.openInputStream(), UTF_8)) {
			String line = rdr.readLine();
			while (line != null) {
				line = processor.getReplacer().process(line);
				pw.println(line);
				line = rdr.readLine();
			}
		} catch (Exception e) {
			return IO.read(resource.openInputStream());
		}
		pw.flush();
		byte[] data = bout.toByteArray();
		return data;
	}
}
