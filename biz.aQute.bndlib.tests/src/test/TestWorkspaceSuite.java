package test;

import aQute.bnd.*;
import aQute.bnd.build.*;
import junit.framework.*;

public class TestWorkspaceSuite 
{
	public static Test suite() 
	{
		TestSuite suite = new TestSuite(TestWorkspace.class);
		return suite;
	}
}
