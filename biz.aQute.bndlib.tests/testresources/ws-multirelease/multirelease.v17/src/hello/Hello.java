package hello;

import org.osgi.service.startlevel.StartLevel;
import org.osgi.annotation.bundle.Requirement;

@Requirement(namespace="fake",name="fake", version="1.2.3")
public class Hello {

	StartLevel ea;
	
	public static void main(String args[]) {
		System.out.println("Hello from java 17");
	}
}
