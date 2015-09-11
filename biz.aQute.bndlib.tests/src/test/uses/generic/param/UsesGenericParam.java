package test.uses.generic.param;

import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

public class UsesGenericParam {

	public void foo(List<Callback> config) {
		Subject subject = new Subject();
	}

}
