package test.uses.param;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

public class UsesParam {
	
	public void foo(Callback config) {
		Subject subject = new Subject();
	}
	
	private void bar(Subject subject) {
		
	}

}
