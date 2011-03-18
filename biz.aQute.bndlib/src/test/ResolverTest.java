package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.resolver.*;

public class ResolverTest extends TestCase {

	public void testSimple() throws Exception {
		Resolver resolver = new Resolver();
		resolver.add( new File( "jar/osgi.core.jar"));
		resolver.add( new File( "jar/ds.jar"));
		Resolution res = resolver.resolve();
		System.out.println(resolver.getErrors());
		System.out.println(resolver.getWarnings());
		
		System.out.println("Unresolved       : " + res.unresolved);
		System.out.println("Unique solutions  : " + res.unique);
		System.out.println("Multiple solutions: " + res.multiple);
	}
}
