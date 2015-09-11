package test.uses.generic.extend;

import java.util.ArrayList;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

@SuppressWarnings("serial")
public class UsesGenericExtend extends ArrayList<Callback> {

	@SuppressWarnings("unused")
	private Subject subject;

}
