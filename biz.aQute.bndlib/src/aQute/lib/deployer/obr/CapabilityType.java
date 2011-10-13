package aQute.lib.deployer.obr;

public enum CapabilityType {
	
	PACKAGE("package"),
	EE("ee"),
	BUNDLE("bundle"),
	MODE("mode"),
	OTHER(null);
	
	private String typeName;

	CapabilityType(String name) {
		this.typeName = name;
	}
	
	public String getTypeName() {
		return typeName;
	}
	
	/**
	 * @throws IllegalArgumentException
	 */
	public static CapabilityType getForTypeName(String typeName) {
		for (CapabilityType type : CapabilityType.values()) {
			if (type.typeName != null && type.typeName.equals(typeName))
				return type;
		}
		throw new IllegalArgumentException("Unknown capability type: " + typeName);
	}
}
