package aQute.bnd.build;

import java.lang.reflect.Method;

import aQute.bnd.service.action.Action;
import aQute.lib.converter.Converter;

public class ReflectAction implements Action {
	String what;

	public ReflectAction(String what) {
		this.what = what;
	}

	@Override
	public void execute(Project project, String action) throws Exception {
		Method m = project.getClass()
			.getMethod(what);
		m.invoke(project);
	}

	@Override
	public void execute(Project project, Object... args) throws Exception {
		for (Method m : project.getClass()
			.getMethods()) {
			if (m.getName()
				.equals(what)) {
				Class<?>[] types = m.getParameterTypes();
				if (args.length == types.length) {
					if (args.length == 0)
						m.invoke(project);
					else {
						try {
							Object[] args2 = new Object[args.length];
							for (int i = 0; i < args.length; i++) {
								args2[i] = Converter.cnv(m.getGenericParameterTypes()[i], args[i]);
							}
							m.invoke(project, args2);
							return;
						} catch (Exception e) {
							// try next method
						}
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return "ReflectAction:[" + what + "]";
	}
}
