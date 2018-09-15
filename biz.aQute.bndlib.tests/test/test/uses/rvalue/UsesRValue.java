package test.uses.rvalue;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

public class UsesRValue {

	public static Callback foo() {
		Subject subject = new Subject();
		return null;
	}

	@SuppressWarnings("unused")
	private static Subject bar() {
		return null;
	}

}
