package aQute.libg.remote.sink;

import java.io.IOException;

import aQute.libg.remote.Source;

class Appender implements Appendable {
	final Source	sources[];
	final String	areaId;
	final boolean	err;

	Appender(Source[] sources, String areaId, boolean err) {
		this.sources = sources;
		this.err = err;
		this.areaId = areaId;
	}

	@Override
	public Appendable append(char ch) throws IOException {
		return append(Character.toString(ch));
	}

	@Override
	public Appendable append(CharSequence text) throws IOException {
		for (Source source : sources) {
			source.output(areaId, text, err);
		}
		return this;
	}

	@Override
	public Appendable append(CharSequence text, int start, int end) throws IOException {
		return append(text.subSequence(start, end));
	}

}
