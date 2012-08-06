package aQute.bnd.osgi;

import java.io.*;

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
		OutputStreamWriter osw = new OutputStreamWriter(bout, Constants.DEFAULT_CHARSET);
		PrintWriter pw = new PrintWriter(osw);
		InputStream in = null;
		BufferedReader rdr = null;
		try {
			in = resource.openInputStream();
			rdr = new BufferedReader(new InputStreamReader(in, "UTF8"));
			String line = rdr.readLine();
			while (line != null) {
				line = processor.getReplacer().process(line);
				pw.println(line);
				line = rdr.readLine();
			}
			pw.flush();
			byte[] data = bout.toByteArray();
			return data;

		}
		finally {
			if (rdr != null) {
				rdr.close();
			}
			if (in != null) {
				in.close();
			}
		}
	}
}
