package name.neilbartlett.eclipse.bndtools.internal.libs;

public interface Function<T, R> {
	R invoke(T arg);
}
