package test.uses.generic.rvalue;

import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

public class UsesGenericRValue {
	public static List<Callback> foo() {
		Subject subject = new Subject();
		return null;
	}

	@SuppressWarnings("unused")
	private static List<Subject> bar() {
		return null;
	}

}
