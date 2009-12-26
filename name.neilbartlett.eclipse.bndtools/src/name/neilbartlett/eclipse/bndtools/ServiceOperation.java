package name.neilbartlett.eclipse.bndtools;

public interface ServiceOperation<R,S,E extends Throwable> {
	R execute(S service) throws E;
}
