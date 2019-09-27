package test.uses.extend;

import javax.security.auth.Subject;
import javax.security.auth.callback.NameCallback;

@SuppressWarnings({
	"serial", "unused"
})
public class UsesExtend extends NameCallback {

	private Subject				subject;
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public UsesExtend(String arg0) {
		super(arg0);
	}

}
