package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;

import aQute.lib.io.*;

public class Section extends Base {

	private int	section;
	private URI	uri;

	public Section(DocumentBuilder g, URI uri) {
		super(g);
		this.uri = uri;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	public void setNumber(int section) {
		this.section = section;

	}

	public int _section(String args[]) {
		return section;
	}

	public String getContent() throws MalformedURLException, IOException {
		return IO.collect(uri.toURL().openStream());
	}
}
