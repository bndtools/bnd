package org.osgi.service.indexer.impl;

public enum MimeType {
	Bundle("application/vnd.osgi.bundle"), Fragment("application/vnd.osgi.bundle"), Jar("application/java-archive");

	private String mimeType;

	MimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public String toString() {
		return mimeType;
	}
}
