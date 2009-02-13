package name.neilbartlett.eclipse.bndtools.internal.libs;

public class ObjectStringifier implements Function<Object, String> {

	public String invoke(Object arg) {
		return arg == null ? "<null>" : arg.toString();
	}

}
