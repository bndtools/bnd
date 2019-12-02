package aQute.bnd.service.result;
class Exceptions {
	static RuntimeException duck(Throwable t) {
		throwsUnchecked(t);
		throw new AssertionError("unreachable");
	}

	@SuppressWarnings("unchecked")
	static <E extends Throwable> void throwsUnchecked(Throwable throwable) throws E {
		throw (E) throwable;
	}
}