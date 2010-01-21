package name.neilbartlett.eclipse.bndtools.frameworks;

public enum OSGiSpecLevel {

	r4_0("R4.0"), r4_1("R4.1"), r4_2("R4.2");

	private final String formattedName;

	OSGiSpecLevel(String formattedName) {
		this.formattedName = formattedName;
	}

	public String getFormattedName() {
		return formattedName;
	}
}
