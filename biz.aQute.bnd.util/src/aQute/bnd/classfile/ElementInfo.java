package aQute.bnd.classfile;

public abstract class ElementInfo {
	public final int			access;
	public final Attribute[]	attributes;

	protected ElementInfo(int access, Attribute[] attributes) {
		this.access = access;
		this.attributes = attributes;
	}
}
