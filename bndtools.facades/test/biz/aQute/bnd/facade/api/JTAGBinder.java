package biz.aQute.bnd.facade.api;

import java.lang.ref.WeakReference;
import java.util.List;

public class JTAGBinder {

	public WeakReference<?> facade(Binder<?> binder) {
		return binder.facade;
	}

	public AutoCloseable reg(Binder<?> binder) {
		return binder.registration.get();
	}

	public boolean isClosed(Binder<?> binder) {
		return binder.closed;
	}

	public List<Binder<?>> binders() {
		return Binder.binders;
	}

	public int cycles(Binder<?> binder) {
		return binder.cycles;
	}

	public FacadeManager facadeManager() {
		return Binder.facadeManager;
	}
}
