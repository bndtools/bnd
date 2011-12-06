/*******************************************************************************
 * Copyright (c) 2011 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package test.diff;

import java.io.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.service.diff.*;
import aQute.lib.osgi.*;

public class TestJarDiff extends TestCase {

	
	public void testClassParsing() throws Exception {

//		ClassInfo classInfo = ClassUtils.getClassInfo(TestClass.class);
//		System.out.println("-->" + classInfo);
	}

	public void testCompare() throws Exception{
		DiffPlugin differ = new DiffPluginImpl();
		
		Diff diff = buildTestJarDiff(differ);
		
		show(diff, 0, true);
		
		
		assertDiff( diff, Delta.MAJOR, Type.BUNDLE, null, null);
		Diff manifest = diff.get("<manifest>");
		Diff export = manifest.get("Export-Package");
		Diff clause = export.get("org.osgi.framework");
		Diff parameter = clause.get("version");
		assertDiff( manifest, Delta.MINOR, Type.MANIFEST, null, null);
		assertDiff( export, Delta.MINOR, Type.HEADER, null, null);
		assertDiff( clause, Delta.CHANGED, Type.CLAUSE, null, null);
		assertDiff( parameter, Delta.CHANGED, Type.PARAMETER, "1.6", "1.5");
	}
	
	private void assertDiff(Diff diff, Delta delta, Type type, String newer, String older) {
		assertNotNull("No diff found", diff);
		assertEquals(delta, diff.getDelta());
		assertEquals(type, diff.getType());
		assertEquals( newer, diff.getNewerValue());
		assertEquals( older, diff.getOlderValue());
	}

	void show(Diff diff, int indent, boolean limited) {
		if (limited && (diff.getDelta() == Delta.UNCHANGED || diff.getDelta() == Delta.IGNORED))
			return;

		 for (int i = 0; i < indent; i++)
			 System.out.print("   ");

		System.out.println(diff.toString());

		if (limited && diff.getDelta().isStructural())
			return;

		for (Diff c : diff.getChildren()) {
			show(c, indent + 1, limited);
		}
	}

	
	public Diff buildTestJarDiff(DiffPlugin differ) throws Exception {
		Builder older = new Builder();
		older.addClasspath( new File("jar/osgi.core.jar"));
		Builder newer = new Builder();
		newer.addClasspath( new File("jar/osgi.core-4.3.0.jar"));
		older.setProperty(Constants.EXPORT_PACKAGE, "org.osgi.framework.*");
		newer.setProperty(Constants.EXPORT_PACKAGE, "org.osgi.framework.*");
		Jar n = newer.build();
		Jar o = older.build();
		
		return differ.diff(n, o);
	}
}
