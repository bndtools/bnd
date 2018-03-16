package aQute.lib.filter;

import junit.framework.TestCase;

public class FilterTest extends TestCase {

	public void testFilters() throws IllegalArgumentException, Exception {
		verify("(&(test=aName)(version>=1.1.0))");
		verify("(&(version>=1.1)(string~=astring))");
		verify("(&(version>=1.1)(long>=99))");
		verify("(&(version>=1.1)(double>=1.0))");
		verify("(&(version>=1.1)(version.list=1.0)(version.list=1.1)(version.list=1.2))");
		verify("(&(version>=1.1)(long.list=1)(long.list=2)(long.list=3)(long.list=4))");
		verify("(&(version>=1.1)(double.list=1.001)(double.list=1.002)(double.list=1.003)(double.list<=1.3))");
		verify("(&(version>=1.1)(string.list~=astring)(string.list~=bstring)(string.list=cString))");
		verify(
			"(&(version>=1.1)(string.list2=a\\\"quote)(string.list2=a\\,comma)(string.list2= aSpace )(string.list2=\\\"start)(string.list2=\\,start)(string.list2=end\\\")(string.list2=end\\,))");
		verify("(&(version>=1.1)(string.list3= aString )(string.list3= bString )(string.list3= cString ))");
		verify("(willResolve=false)");
	}

	private void verify(String string) throws IllegalArgumentException, Exception {
		assertNull("Invalid filter", new Filter(string).verify());

	}
}
