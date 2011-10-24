/*
 * $Header$
 * 
 * Copyright (c) OSGi Alliance (2007). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package test;

import junit.framework.*;

import org.osgi.impl.bundle.obr.resource.*;

public class TestVersionImpl extends TestCase {
	
	public void testVersion() {
		tv("[0.0.0,1.1.1]");
		tv("(0.0.0,1.1.1)");
		tv("[0.0.0,1.1.1)");
		tv("(0.0.0,1.1.1]");
		tv("(0.0.0,1.1.1]");
		try {
			tv("(2.0.0,1.1.1]");
			fail("Invalid range succeeded");
		} catch( IllegalArgumentException e) {
			// Ok
		}
	}

	void tv(String string) {
		VersionRange v = new VersionRange(string);
		String s = v.toString();
		assertEquals(string,s);
	}

}
