package test.uses.exception;

import javax.security.auth.Subject;
import javax.security.auth.callback.UnsupportedCallbackException;

public class UsesException {
	public void foo() throws UnsupportedCallbackException {
		Subject subject = new Subject();
	}

}
