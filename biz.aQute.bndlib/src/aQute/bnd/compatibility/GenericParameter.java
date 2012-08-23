package aQute.bnd.compatibility;

public class GenericParameter {
	String		name;
	GenericType	bounds[];

	public GenericParameter(String name, GenericType[] bounds) {
		this.name = name;
		this.bounds = bounds;
		if (bounds == null || bounds.length == 0)
			this.bounds = new GenericType[] {
				new GenericType(Object.class)
			};
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		if (bounds != null && bounds.length > 0) {
			for (GenericType gtype : bounds) {
				sb.append(":");
				sb.append(gtype);
			}
		}
		return sb.toString();
	}
}
