package name.neilbartlett.eclipse.bndtools.editor.model;

public class ComponentSvcReference {
	private final String name;
	private final String bind;
	private final String unbind;
	private final String serviceClass;
	private final boolean optional;
	private final boolean multiple;
	private final boolean dynamic;
	private final String targetFilter;

	public ComponentSvcReference(String name, String bind, String unbind,
			String serviceClass, boolean optional, boolean multiple,
			boolean dynamic, String targetFilter) {
		assert serviceClass != null : "serviceClass may not be null";
		
		this.name = name;
		this.bind = bind;
		this.unbind = unbind;
		this.serviceClass = serviceClass;
		this.optional = optional;
		this.multiple = multiple;
		this.dynamic = dynamic;
		this.targetFilter = targetFilter;
	}

	public String getName() {
		return name;
	}

	public String getBind() {
		return bind;
	}

	public String getUnbind() {
		return unbind;
	}

	public String getServiceClass() {
		return serviceClass;
	}

	public boolean isOptional() {
		return optional;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public String getTargetFilter() {
		return targetFilter;
	}
}