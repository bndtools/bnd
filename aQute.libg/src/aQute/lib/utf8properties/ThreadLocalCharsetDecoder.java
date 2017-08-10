package aQute.lib.utf8properties;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

class ThreadLocalCharsetDecoder extends ThreadLocal<CharsetDecoder> {
	private final Charset charset;

	ThreadLocalCharsetDecoder(Charset charset) {
		super();
		this.charset = charset;
	}

	@Override
	protected CharsetDecoder initialValue() {
		return charset.newDecoder();
	}
}
