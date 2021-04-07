package aQute.bnd.gradle;

public class BndPluginExtension {
	private String	exportFilter	= ".*.bndrun";
	private String	resolveFilter	= ".*.bndrun";
	private String	testrunFilter	= ".*.bndrun";

	public String getTestrunFilter() {
		return testrunFilter;
	}

	public void setTestrunFilter(String testrunFilter) {
		this.testrunFilter = testrunFilter;
	}

	public String getExportFilter() {
		return exportFilter;
	}

	public void setExportFilter(String exportFilter) {
		this.exportFilter = exportFilter;
	}

	public String getResolveFilter() {
		return resolveFilter;
	}

	public void setResolveFilter(String resolveFilter) {
		this.resolveFilter = resolveFilter;
	}

}
