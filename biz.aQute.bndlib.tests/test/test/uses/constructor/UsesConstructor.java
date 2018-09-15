package test.uses.constructor;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

public class UsesConstructor {

	public UsesConstructor(CallbackHandler ch) {
		Subject subject = new Subject();
	}

	@SuppressWarnings("unused")
	private UsesConstructor(Subject subject) {

	}

}
