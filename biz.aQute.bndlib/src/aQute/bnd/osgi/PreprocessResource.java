package aQute.bnd.osgi;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;

public class PreprocessResource extends AbstractResource {
	private final Resource	resource;
	private final Processor	processor;

	public PreprocessResource(Processor processor, Resource r) {
		super(r.lastModified());
		this.processor = processor;
		this.resource = r;
		setExtra(resource.getExtra());
	}

	@Override
	protected byte[] getBytes() throws Exception {
		try (ByteBufferOutputStream bout = new ByteBufferOutputStream();
			PrintWriter pw = IO.writer(bout, Constants.DEFAULT_CHARSET)) {
			ByteBuffer bb = resource.buffer();
			BufferedReader r;
			if (bb != null) {
				r = IO.reader(bb, Constants.DEFAULT_CHARSET);
			} else {
				r = IO.reader(resource.openInputStream(), Constants.DEFAULT_CHARSET);
			}
			try (BufferedReader rdr = r) {
				String line = rdr.readLine();
				while (line != null) {
					line = processor.getReplacer()
						.process(line);
					pw.println(line);
					line = rdr.readLine();
				}
			} catch (Exception e) {
				bb = resource.buffer();
				if (bb != null) {
					return IO.read(bb);
				} else {
					return IO.read(resource.openInputStream());
				}
			}
			pw.flush();
			byte[] data = bout.toByteArray();
			return data;
		}
	}
}
