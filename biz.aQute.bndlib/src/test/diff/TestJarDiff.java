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
import aQute.lib.jardiff.*;
import aQute.lib.osgi.*;

public class TestJarDiff extends TestCase {

	
	public void testClassParsing() throws Exception {

//		ClassInfo classInfo = ClassUtils.getClassInfo(TestClass.class);
//		System.out.println("-->" + classInfo);
	}

	public void testCompare() throws Exception{
		
		JarDiff diff = buildTestJarDiff();
		
		JarDiff.printDiff(diff, new PrintWriter(System.out));
		
		diff.calculateVersions();
		
		JarDiff.printDiff(diff, new PrintWriter(System.out));
		
	}
	
	public static JarDiff buildTestJarDiff() throws Exception {
		
		String bsn = "test.diff.classes";
		String exportedPackages1 = "test.diff.classes.majorchange;version=1.0.0,test.diff.classes.minorchange;version=1.0.0,test.diff.classes.newpackage";
		String exportedPackages2 = "test.diff.classes.majorchange;version=1.0.0,test.diff.classes.minorchange;version=1.0.0,test.diff.classes.deletedpackage;version=1.0.0";

		Jar newJar = new Jar(bsn);
		Manifest mf1 = new Manifest();
		mf1.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mf1.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, bsn + ";singleton:=true");
		mf1.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportedPackages1);
		mf1.getMainAttributes().putValue("NewValueAdded", "new");
		mf1.getMainAttributes().putValue("Unmodified", "true");
		mf1.getMainAttributes().putValue("Modified", "true");
		
		Resource newManifest = Utils.createResource(mf1, null);
		newJar.putResource("META-INF/MANIFEST.MF", newManifest);
		//newJar.getManifest();
		
		Jar oldJar = new Jar(bsn);
		Manifest mf2 = new Manifest();
		mf2.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mf2.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, bsn);
		mf2.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0.0.201010101010");
		mf2.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportedPackages2);
		mf2.getMainAttributes().putValue("OldValueRemoved", "old");
		mf2.getMainAttributes().putValue("Unmodified", "true");
		mf2.getMainAttributes().putValue("Modified", "false");

		Resource oldManifest = Utils.createResource(mf2, null);
		oldJar.putResource("META-INF/MANIFEST.MF", oldManifest);
		//oldJar.getManifest();
		
		Resource unmodified = Utils.createResource(test.diff.classes.oldclasses.UnmodifiedClass.class, null);
		Resource oldMinor = Utils.createResource(test.diff.classes.oldclasses.MinorModifiedClass.class, null);
		Resource newMinor = Utils.createResource(test.diff.classes.newclasses.MinorModifiedClass.class, null);
		Resource oldMajor = Utils.createResource(test.diff.classes.oldclasses.MajorModifiedClass.class, null);
		Resource newMajor = Utils.createResource(test.diff.classes.newclasses.MajorModifiedClass.class, null);
		
		// Major changes
		String packageName = "test.diff.classes.majorchange";
		addResourceToJar(packageName, "UnmodifiedClass", newJar, unmodified);
		addResourceToJar(packageName, "UnmodifiedClass", oldJar, unmodified);
		addResourceToJar(packageName, "ModifiedClass", newJar, newMajor);
		addResourceToJar(packageName, "ModifiedClass", oldJar, oldMajor);
		
		// Minor changes
		packageName = "test.diff.classes.minorchange";
		addResourceToJar(packageName, "UnmodifiedClass", newJar, unmodified);
		addResourceToJar(packageName, "UnmodifiedClass", oldJar, unmodified);
		addResourceToJar(packageName, "ModifiedClass", newJar, newMinor);
		addResourceToJar(packageName, "ModifiedClass", oldJar, oldMinor);
		
		// New package
		packageName = "test.diff.classes.newpackage";
		addResourceToJar(packageName, "NewClass", newJar, unmodified);

		// Deleted package
		packageName = "test.diff.classes.deletedpackage";
		addResourceToJar(packageName, "DeletedClass", oldJar, unmodified);
		
		return JarDiff.diff(newJar, oldJar);
	}

	static void addResourceToJar(String packageName, String className, Jar jar, Resource resource) {
		String qualifiedName = getQualifiedName(packageName, className);
		jar.putResource(qualifiedName.replace('.', '/') + ".class", resource);
	}
	
	static String getQualifiedName(String packageName, String className) {
		return packageName + "." + className;
	}
}
