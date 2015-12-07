package org.bndtools.templating;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringResource implements Resource {
	
	private final String content;

	public StringResource(String content) {
		this.content = content;
	}

	@Override
	public InputStream getContent() throws IOException {
		return new ByteArrayInputStream(content.getBytes("UTF-8"));
	}

	@Override
	public String getTextEncoding() {
		return "UTF-8";
	}

}
