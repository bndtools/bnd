package biz.aQute.bndoc.lib;
public enum PageSize {
	A4("210mm 297mm"), LETTER("8.5in 11in"), LEGAL("8.5in 14in");

	private String	size;

	PageSize(String size) {
		this.size = size;

	}

	public String size() {
		return size;
	}
}

