package org.bndtools.templating;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class BytesResource implements Resource {
	
	private final byte[] data;
	private final String encoding;
	
	public BytesResource(byte[] data, String encoding) {
		this.data = data;
		this.encoding = encoding;
	}
	
	public BytesResource(byte[] data) {
		this.data = data;
		this.encoding = Charset.defaultCharset().name();
	}
	
	public static BytesResource loadFrom(InputStream input) throws IOException {
		return loadFrom(input, Charset.defaultCharset().name());
	}
	
	public static BytesResource loadFrom(InputStream input, String encoding) throws IOException {
		byte[] buf = new byte[1024];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int count = input.read(buf);
		while (count >= 0) {
			out.write(buf, 0, count);
			count = input.read(buf);
		}
		return new BytesResource(out.toByteArray(), encoding);
	}

	@Override
	public InputStream getContent() throws IOException {
		return new ByteArrayInputStream(data);
	}

	@Override
	public String getTextEncoding() {
		return encoding;
	}

}
