package aQute.bnd.osgi.providertype;

import org.osgi.annotation.versioning.ProviderType;

public class TestProviderInheritance {
	@ProviderType
	public static class Top {}

	public static class Middle extends Top {}

	public static class Bottom extends Middle {}

	public static class None {}

}
