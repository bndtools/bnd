package aQute.bnd.classfile;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class ElementInfo {
	public final int			access;
	public final Attribute[]	attributes;

	protected ElementInfo(int access, Attribute[] attributes) {
		this.access = access;
		this.attributes = attributes;
	}

	public <T> Optional<T> getAttribute(Class<T> type) {
		return Stream.of(attributes)
			.filter(type::isInstance)
			.map(type::cast)
			.findFirst();
	}

}
