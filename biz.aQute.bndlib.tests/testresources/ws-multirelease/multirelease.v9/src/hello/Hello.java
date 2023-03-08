package hello;

import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;

public class Hello {
	ConditionalPermissionAdmin cpa;
	Private local = new Private();
	public static void main(String args[]) {
		System.out.println("Hello from java 9");
	}
}
