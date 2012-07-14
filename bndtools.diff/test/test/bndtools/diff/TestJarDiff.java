/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package test.bndtools.diff;

import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.objectweb.asm.ClassWriter;

import test.bndtools.diff.util.ByteArrayResource;
import test.bndtools.diff.util.ClassBuilder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import bndtools.diff.JarDiff;

public class TestJarDiff extends TestCase {

	public static void testCompare() throws Exception{
		
		JarDiff diff = buildTestJarDiff();
		
		diff.compare();
		JarDiff.printDiff(diff, System.out);
		
		diff.calculatePackageVersions();
		
		JarDiff.printDiff(diff, System.out);
		
	}
	
	public static JarDiff buildTestJarDiff() {
		
		String bsn = "test";
		String exportedPackages1 = "test.majorModifiedPackage;version=1.0.0,test.minorModifiedPackage;version=1.0.0,test.newPackage";
		String exportedPackages2 = "test.majorModifiedPackage;version=1.0.0,test.minorModifiedPackage;version=1.0.0,test.deletedPackage;version=1.0.0";

		Jar newJar = new Jar(bsn);
		Manifest mf1 = new Manifest();
		mf1.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, bsn + ";singleton:=true");
		mf1.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportedPackages1);
		newJar.setManifest(mf1);

		Jar oldJar = new Jar(bsn);
		Manifest mf2 = new Manifest();
		mf2.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, bsn + ";singleton:=true");
		mf2.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0.0.201010101010");
		mf2.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, exportedPackages2);
		oldJar.setManifest(mf2);

		// Major change
		String packageName = "test.majorModifiedPackage";
		String className = "UnmodifiedClass";
		String qualifiedName = getQualifiedName(packageName, className);
		
		ClassWriter ver1 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver1, "STRING1", String.class);
		ClassBuilder.addMethod(ver1, void.class, "method1", String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "method2", String.class, String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "method3", String.class, String.class);
		ByteArrayResource unmodified = new ByteArrayResource(ClassBuilder.endClass(ver1));
		newJar.putResource(qualifiedName.replace('.', '/') + ".class", unmodified);
		oldJar.putResource(qualifiedName.replace('.', '/') + ".class", unmodified);
		
		className = "ModifiedClass";
		qualifiedName = getQualifiedName(packageName, className);
		
		ver1 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver1, "STRING1", String.class);
		ClassBuilder.addMethod(ver1, void.class, "newParam", String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "removedParam", String.class, String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "overloaded", String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "overloaded", String.class, String.class, String.class);
		newJar.putResource(qualifiedName.replace('.', '/') + ".class", new ByteArrayResource(ClassBuilder.endClass(ver1)));
		
		ClassWriter ver2 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver2, "STRING1", String.class);
		ClassBuilder.addStaticFinalField(ver2, "STRING2", String.class);
		ClassBuilder.addMethod(ver2, void.class, "newParam", String.class, String.class, String.class);
		ClassBuilder.addMethod(ver2, void.class, "removedParam", String.class, String.class);
		ClassBuilder.addMethod(ver2, void.class, "overloaded", String.class, String.class);
		oldJar.putResource(qualifiedName.replace('.', '/') + ".class", new ByteArrayResource(ClassBuilder.endClass(ver2)));

		
		// Minor change
		packageName = "test.minorModifiedPackage";
		className = "UnmodifiedClass";
		qualifiedName = getQualifiedName(packageName, className);
		
		ver1 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver1, "STRING1", String.class);
		ClassBuilder.addMethod(ver1, void.class, "method1", String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "method2", String.class, String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "method3", String.class, String.class);
		unmodified = new ByteArrayResource(ClassBuilder.endClass(ver1));
		newJar.putResource(qualifiedName.replace('.', '/') + ".class", unmodified);
		oldJar.putResource(qualifiedName.replace('.', '/') + ".class", unmodified);
		
		className = "ModifiedClass";
		qualifiedName = getQualifiedName(packageName, className);
		
		ver1 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver1, "STRING1", String.class);
		ClassBuilder.addMethod(ver1, void.class, "overloaded", String.class, String.class);
		ClassBuilder.addMethod(ver1, void.class, "overloaded", String.class, String.class, String.class);
		newJar.putResource(qualifiedName.replace('.', '/') + ".class", new ByteArrayResource(ClassBuilder.endClass(ver1)));
		
		ver2 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver2, "STRING1", String.class);
		//ClassBuilder.addStaticFinalField(ver2, "STRING2", String.class);
		ClassBuilder.addMethod(ver2, void.class, "overloaded", String.class, String.class);
		oldJar.putResource(qualifiedName.replace('.', '/') + ".class", new ByteArrayResource(ClassBuilder.endClass(ver2)));

		
		// New package
		packageName = "test.newPackage";
		className = "NewClass";
		qualifiedName = getQualifiedName(packageName, className);

		ver1 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver1, "STRING1", String.class);
		ClassBuilder.addMethod(ver1, void.class, "method", String.class, String.class);
		newJar.putResource(qualifiedName.replace('.', '/') + ".class", new ByteArrayResource(ClassBuilder.endClass(ver1)));

		// Deleted package
		packageName = "test.deletedPackage";
		className = "DeletedClass";
		qualifiedName = getQualifiedName(packageName, className);
		
		ver1 = ClassBuilder.createInterface(qualifiedName);
		ClassBuilder.addStaticFinalField(ver1, "STRING1", String.class);
		ClassBuilder.addMethod(ver1, void.class, "method", String.class, String.class);
		newJar.putResource(qualifiedName.replace('.', '/') + ".class", new ByteArrayResource(ClassBuilder.endClass(ver1)));

		
		JarDiff diff = new JarDiff(newJar, oldJar);
		return diff;
	}

	private static String getQualifiedName(String packageName, String className) {
		return packageName + "." + className;
	}
}
