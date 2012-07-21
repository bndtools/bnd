package test.uses.rvalue;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

public class UsesRValue {
	
	public Callback foo(){
		Subject subject = new Subject();
		return null;
	}
	
	private Subject bar() {
		return null;
	}

}
