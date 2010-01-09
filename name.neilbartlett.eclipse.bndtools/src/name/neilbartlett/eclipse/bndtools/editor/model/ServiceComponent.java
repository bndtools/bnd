package name.neilbartlett.eclipse.bndtools.editor.model;

public class ServiceComponent {
	
	private final String pattern;
	private final ServiceComponentAttribs attribs;

	public ServiceComponent(String pattern, ServiceComponentAttribs attribs) {
		this.pattern = pattern;
		this.attribs = attribs;
	}

	public String getPattern() {
		return pattern;
	}

	public ServiceComponentAttribs getAttribs() {
		return attribs;
	}
}
