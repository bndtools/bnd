package test.uses.extend;

import javax.security.auth.Subject;
import javax.security.auth.callback.NameCallback;

@SuppressWarnings({
	"serial", "unused"
})
public class UsesExtend extends NameCallback {

	private Subject subject;

	public UsesExtend(String arg0) {
		super(arg0);
	}

}
