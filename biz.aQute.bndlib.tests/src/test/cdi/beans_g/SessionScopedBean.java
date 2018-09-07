package test.cdi.beans_g;

import javax.enterprise.context.SessionScoped;

import org.osgi.service.cdi.annotations.Reference;

@SessionScoped
public class SessionScopedBean {

	@Reference
	Buz buz;
}
