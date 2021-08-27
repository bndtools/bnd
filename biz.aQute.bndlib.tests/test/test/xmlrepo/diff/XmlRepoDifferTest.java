package test.xmlrepo.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.Test;

import aQute.bnd.differ.XmlRepoDiffer;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.lib.io.IO;

public class XmlRepoDifferTest {

	@Test
	public void testXmlRepoDifferences_NO_FILTER_EXPANSION() throws Exception {
		Tree newerTree = XmlRepoDiffer.resource(IO.getFile("testresources/xmlrepo/newer.xml"));
		Tree olderTree = XmlRepoDiffer.resource(IO.getFile("testresources/xmlrepo/older.xml"));

		Diff diff = newerTree.diff(olderTree);
		Collection<Diff> resources = diff.getChildren();

		for (Diff resourceDiff : resources) {
			String resourceName = resourceDiff.getName();
			Collection<Diff> resourceElements = resourceDiff.getChildren();

			if (resourceName.equals("org.apache.felix.cm.json")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.UNCHANGED);

				for (Diff resourceElement : resourceElements) {
					Type type = resourceElement.getType();
					if (type == Type.REQUIREMENTS || type == Type.CAPABILITIES || type == Type.VERSION) {
						assertThat(resourceElement.getDelta()).isEqualTo(Delta.UNCHANGED);
					}
				}
			}
			if (resourceName.equals("org.apache.felix.configadmin")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.MAJOR);

				for (Diff resourceElement : resourceElements) {
					Type type = resourceElement.getType();
					Delta delta = resourceElement.getDelta();
					if (type == Type.REQUIREMENTS) {
						assertThat(delta).isEqualTo(Delta.UNCHANGED);
					}
					if (type == Type.CAPABILITIES) {
						assertThat(delta).isEqualTo(Delta.MAJOR);

						Diff bndMavenCapDiff = resourceElement
							.get("bnd.maven:org.apache.felix:org.apache.felix.configadmin");
						assertThat(bndMavenCapDiff.getDelta()).isEqualTo(Delta.MAJOR);

						Collection<Diff> bndMavenCapDiffAttributes = bndMavenCapDiff.getChildren();
						for (Diff df : bndMavenCapDiffAttributes) {
							String name = df.getName();
							if (df.getType() == Type.ATTRIBUTE) {
								if (df.getDelta() == Delta.ADDED) {
									assertThat(name).isEqualTo("maven-version:1.9.22");
								}
								if (df.getDelta() == Delta.REMOVED) {
									assertThat(name).isEqualTo("maven-version:1.9.10");
								}
							}
						}

						Diff identityCapDiff = resourceElement.get("osgi.identity:org.apache.felix.configadmin");
						assertThat(identityCapDiff.getDelta()).isEqualTo(Delta.MAJOR);

						Collection<Diff> identityCapDiffAttributes = identityCapDiff.getChildren();
						for (Diff df : identityCapDiffAttributes) {
							String name = df.getName();
							if (df.getType() == Type.ATTRIBUTE) {
								if (df.getDelta() == Delta.ADDED) {
									assertThat(name).isEqualTo("version:1.9.22");
								}
								if (df.getDelta() == Delta.REMOVED) {
									assertThat(name).isEqualTo("version:1.9.10");
								}
							}
						}

					}
					if (type == Type.VERSION) {
						assertThat(delta).isIn(Delta.ADDED, Delta.REMOVED);
						if (delta == Delta.REMOVED) {
							assertThat(resourceElement.getNewer()).isNull();
							assertThat(resourceElement.getOlder()
								.getName()).isEqualTo("1.9.10");
						}
						if (delta == Delta.ADDED) {
							assertThat(resourceElement.getOlder()).isNull();
							assertThat(resourceElement.getNewer()
								.getName()).isEqualTo("1.9.22");
						}
					}
				}
			}
			if (resourceName.equals("org.apache.felix.converter")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
			}
			if (resourceName.equals("org.apache.felix.eventadmin")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.MAJOR);

				Diff eeCapDiff = resourceDiff.get("<requirements>")
					.get("osgi.ee:JavaSE");
				for (Diff eeCapDirective : eeCapDiff.getChildren()) {
					if (eeCapDirective.getType() == Type.DIRECTIVE) {
						String name = eeCapDirective.getName();
						if (eeCapDirective.getDelta() == Delta.REMOVED) {
							assertThat(name).isEqualTo("filter:(&(osgi.ee=JavaSE)(version=1.7))");
						}
						if (eeCapDirective.getDelta() == Delta.ADDED) {
							assertThat(name).isEqualTo("filter:(&(osgi.ee=JavaSE)(version=1.8))");
						}
					}
				}
			}
			if (resourceName.equals("org.apache.felix.framework")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
			}
			if (resourceName.equals("org.apache.felix.http.jetty")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
			}
			if (resourceName.equals("org.apache.felix.http.servlet-api")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
			}
			if (resourceName.equals("org.apache.sling.commons.johnzon")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
			}
			if (resourceName.equals("org.osgi.util.function")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
			}
			if (resourceName.equals("org.osgi.util.promise")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.REMOVED);
			}
			if (resourceName.equals("org.osgi.util.pushstream")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.REMOVED);

				for (Diff resourceElement : resourceElements) {
					Type type = resourceElement.getType();
					Collection<Diff> children = resourceElement.getChildren();
					if (type == Type.REQUIREMENTS) {
						assertThat(resourceElement.getDelta()).isEqualTo(Delta.REMOVED);

						Diff eeDiff = resourceElement.get("osgi.ee:JavaSE/compact1");
						assertThat(eeDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff package1Diff = resourceElement.get("osgi.wiring.package:org.osgi.util.function");
						assertThat(package1Diff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff package1FilterDiff = package1Diff.get(
								"filter:(&(osgi.wiring.package=org.osgi.util.function)(version>=1.1.0)(!(version>=2.0.0)))");
						assertThat(package1FilterDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff package2Diff = resourceElement.get("osgi.wiring.package:org.osgi.util.promise");
						assertThat(package1Diff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff package2FilterDiff = package2Diff.get(
								"filter:(&(osgi.wiring.package=org.osgi.util.promise)(version>=1.1.0)(!(version>=2.0.0)))");
						assertThat(package2FilterDiff.getDelta()).isEqualTo(Delta.REMOVED);
					}
					if (type == Type.CAPABILITIES) {
						assertThat(resourceElement.getDelta()).isEqualTo(Delta.REMOVED);

						Diff bndMavenCapDiff = resourceElement.get("bnd.maven:org.osgi:org.osgi.util.pushstream");
						assertThat(bndMavenCapDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff mvnClassifierDiff = bndMavenCapDiff.get("maven-classifier:");
						assertThat(mvnClassifierDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff mvnExtensionDiff = bndMavenCapDiff.get("maven-extension:jar");
						assertThat(mvnExtensionDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff mvnRepoDiff = bndMavenCapDiff.get("maven-repository:Runtime");
						assertThat(mvnRepoDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff mvnVersionDiff = bndMavenCapDiff.get("maven-version:1.0.1");
						assertThat(mvnVersionDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiContentCapDiff = resourceElement
							.get("osgi.content:1E0C9D435A107444A4461788E62BDDC94715E444AFDBC54417593ECA4BB50CE2");
						assertThat(bndMavenCapDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiContentCapMimeDiff = osgiContentCapDiff.get("mime:application/vnd.osgi.bundle");
						assertThat(osgiContentCapMimeDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiContentCapSizeDiff = osgiContentCapDiff.get("size:132226");
						assertThat(osgiContentCapSizeDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff identityCapDiff = resourceElement.get("osgi.identity:org.osgi.util.pushstream");
						assertThat(identityCapDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff identityCapCopyrightDiff = identityCapDiff
							.get("copyright:Copyright (c) OSGi Alliance (2000, 2018). All Rights Reserved.");
						assertThat(identityCapCopyrightDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff identityCapTypeDiff = identityCapDiff.get("type:osgi.bundle");
						assertThat(identityCapTypeDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff identityCapDescDiff = identityCapDiff
							.get("description:OSGi Companion Code for org.osgi.util.pushstream Version 1.0.1");
						assertThat(identityCapDescDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff identityCapDocDiff = identityCapDiff.get("documentation:https://www.osgi.org/");
						assertThat(identityCapDocDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff identityCapLicenseDiff = identityCapDiff.get(
							"license:Apache-2.0; link=\"http://www.apache.org/licenses/LICENSE-2.0\"; description=\"Apache License, Version 2.0\"");
						assertThat(identityCapLicenseDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiWiringBundleCapDiff = resourceElement
							.get("osgi.wiring.bundle:org.osgi.util.pushstream");
						assertThat(osgiWiringBundleCapDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiWiringBundleVersionCapDiff = osgiWiringBundleCapDiff
							.get("bundle-version:1.0.1.201810101357");
						assertThat(osgiWiringBundleVersionCapDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiWiringPackageCapDiff = resourceElement
							.get("osgi.wiring.package:org.osgi.util.pushstream");
						assertThat(osgiWiringPackageCapDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiWiringPackageCapBndHashesDiff = osgiWiringPackageCapDiff.get("bnd.hashes:-1923478059");
						assertThat(osgiWiringPackageCapBndHashesDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiWiringPackageCapVersionDiff = osgiWiringPackageCapDiff
							.get("bundle-version:1.0.1.201810101357");
						assertThat(osgiWiringPackageCapVersionDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiWiringPackageCapBsnDiff = osgiWiringPackageCapDiff
							.get("bundle-symbolic-name:org.osgi.util.pushstream");
						assertThat(osgiWiringPackageCapBsnDiff.getDelta()).isEqualTo(Delta.REMOVED);

						Diff osgiWiringPackageCapUsesDiff = osgiWiringPackageCapDiff
							.get("uses:org.osgi.util.function,org.osgi.util.promise");
						assertThat(osgiWiringPackageCapUsesDiff.getDelta()).isEqualTo(Delta.REMOVED);
					}
					if (type == Type.VERSION) {
						assertThat(resourceElement.getDelta()).isEqualTo(Delta.REMOVED);
					}
				}
			}
		}
	}

	@Test
	public void testXmlRepoDifferences_FILTER_EXPANSION() throws Exception {
		Tree newerTree = XmlRepoDiffer.resource(IO.getFile("testresources/xmlrepo/newer.xml"), true);
		Tree olderTree = XmlRepoDiffer.resource(IO.getFile("testresources/xmlrepo/older.xml"), true);

		Diff diff = newerTree.diff(olderTree);
		Collection<Diff> resources = diff.getChildren();

		for (Diff resourceDiff : resources) {
			String resourceName = resourceDiff.getName();
			Collection<Diff> resourceElements = resourceDiff.getChildren();

			if (resourceName.equals("org.apache.felix.configadmin")) {
				assertThat(resourceDiff.getDelta()).isEqualTo(Delta.MAJOR);

				for (Diff resourceElement : resourceElements) {
					Type type = resourceElement.getType();
					if (type == Type.REQUIREMENTS) {
						Diff eeDiff = resourceElement.get("osgi.ee:JavaSE");
						Diff filterDiff = eeDiff.get("<filter>");

						Diff filterOpDiff = filterDiff.get("op:=");
						Diff filterEeDiff = filterDiff.get("osgi.ee:JavaSE");
						Diff filterVersionDiff = filterDiff.get("version:1.7");

						assertThat(filterOpDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
						assertThat(filterEeDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
						assertThat(filterVersionDiff.getDelta()).isEqualTo(Delta.UNCHANGED);

						assertThat(filterOpDiff.getType()).isEqualTo(Type.EXPRESSION);
						assertThat(filterEeDiff.getType()).isEqualTo(Type.EXPRESSION);
						assertThat(filterVersionDiff.getType()).isEqualTo(Type.EXPRESSION);

						Diff serviceDiff = resourceElement.get("osgi.service:org.osgi.service.log.LogService");
						Diff filterServiceDiff = serviceDiff.get("<filter>");

						Diff filterServiceOpDiff = filterServiceDiff.get("op:=");
						Diff filterServiceClassDiff = filterServiceDiff
							.get("objectClass:org.osgi.service.log.LogService");

						Diff effectiveFilter = serviceDiff.get("effective:active");
						Diff resolutionFilter = serviceDiff.get("resolution:optional");

						assertThat(filterServiceOpDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
						assertThat(filterServiceClassDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
						assertThat(effectiveFilter.getDelta()).isEqualTo(Delta.UNCHANGED);
						assertThat(filterServiceClassDiff.getDelta()).isEqualTo(Delta.UNCHANGED);

						assertThat(filterServiceOpDiff.getType()).isEqualTo(Type.EXPRESSION);
						assertThat(filterServiceClassDiff.getType()).isEqualTo(Type.EXPRESSION);
						assertThat(effectiveFilter.getType()).isEqualTo(Type.FILTER);
						assertThat(resolutionFilter.getType()).isEqualTo(Type.FILTER);

						Diff packageDiff = resourceElement.get("osgi.wiring.package:org.apache.felix.cm");
						Diff filterPackageDiff = packageDiff.get("<filter>");

						Diff filterPackageNameDiff = filterPackageDiff.get("package:org.apache.felix.cm");
						Diff filterPackageRangeDiff = filterPackageDiff.get("range:version=[1.2.0,2.0.0)");

						assertThat(filterPackageNameDiff.getDelta()).isEqualTo(Delta.UNCHANGED);
						assertThat(filterPackageRangeDiff.getDelta()).isEqualTo(Delta.UNCHANGED);

						assertThat(filterPackageNameDiff.getType()).isEqualTo(Type.EXPRESSION);
						assertThat(filterPackageRangeDiff.getType()).isEqualTo(Type.EXPRESSION);
					}
				}
			}
		}
	}

}
