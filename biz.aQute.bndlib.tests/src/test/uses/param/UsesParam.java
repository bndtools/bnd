package test.uses.param;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

public class UsesParam {

	public static void foo(Callback config) {
		Subject subject = new Subject();
	}

	@SuppressWarnings("unused")
	private static void bar(Subject subject) {

	}

}
