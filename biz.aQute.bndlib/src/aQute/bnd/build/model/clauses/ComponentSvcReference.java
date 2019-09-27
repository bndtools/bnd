package aQute.bnd.build.model.clauses;

public class ComponentSvcReference implements Cloneable {

	private String	name;
	private String	bind;
	private String	unbind;
	private String	serviceClass;
	private boolean	optional;
	private boolean	multiple;
	private boolean	dynamic;
	private String	targetFilter;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBind() {
		return bind;
	}

	public void setBind(String bind) {
		this.bind = bind;
	}

	public String getUnbind() {
		return unbind;
	}

	public void setUnbind(String unbind) {
		this.unbind = unbind;
	}

	public String getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(String serviceClass) {
		this.serviceClass = serviceClass;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public String getTargetFilter() {
		return targetFilter;
	}

	public void setTargetFilter(String targetFilter) {
		this.targetFilter = targetFilter;
	}

	@Override
	public ComponentSvcReference clone() {
		ComponentSvcReference copy = new ComponentSvcReference();
		copy.name = this.name;
		copy.serviceClass = this.serviceClass;
		copy.bind = this.bind;
		copy.unbind = this.unbind;
		copy.optional = this.optional;
		copy.multiple = this.multiple;
		copy.dynamic = this.dynamic;
		copy.targetFilter = this.targetFilter;
		return copy;
	}

	public void copyFrom(ComponentSvcReference other) {
		this.name = other.name;
		this.serviceClass = other.serviceClass;
		this.bind = other.bind;
		this.unbind = other.unbind;
		this.optional = other.optional;
		this.multiple = other.multiple;
		this.dynamic = other.dynamic;
		this.targetFilter = other.targetFilter;
	}
}
