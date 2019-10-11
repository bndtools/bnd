/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package aQute.tester.junit.platform.reporting.legacy;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Utility methods for dealing with legacy reporting infrastructure, such as
 * reporting systems built on the Ant-based XML reporting format for JUnit 4.
 * <p>
 * Originally copied from JUnit's
 * <tt>org.junit.platform:junit-platform-launcher:1.5.0</tt>. Moved to a new
 * package to suit the OSGi environment of
 * <tt>biz.aQute.tester.junit-platform</tt>.
 * <p>
 * Copied {@link #STDOUT_REPORT_ENTRY_KEY} and {@link #STDERR_REPORT_ENTRY_KEY}
 * from <tt>org.junit.platform.launcher.LauncherConstants</tt> to avoid creating
 * dependency on <tt>org.junit.platform.launcher</tt> > 1.0.0, and dropped the
 * API guardian usage to avoid the additional dependency.
 *
 * @since 1.0.3
 */
public class LegacyReportingUtils {

	private LegacyReportingUtils() {
		/* no-op */
	}

	/**
	 * Key used to publish captured output to {@link System#out} as part of a
	 * {@link ReportEntry}: {@value}
	 * <p>
	 * Copied from org.junit.platform.launcher.LauncherConstants
	 */
	public static final String	STDOUT_REPORT_ENTRY_KEY	= "stdout";

	/**
	 * Key used to publish captured output to {@link System#err} as part of a
	 * {@link ReportEntry}: {@value}
	 * <p>
	 * Copied from org.junit.platform.launcher.LauncherConstants
	 */
	public static final String	STDERR_REPORT_ENTRY_KEY	= "stderr";

	/**
	 * Get the class name for the supplied {@link TestIdentifier} using the
	 * supplied {@link TestPlan}.
	 * <p>
	 * This implementation attempts to find the closest test identifier with a
	 * {@link ClassSource} by traversing the hierarchy upwards towards the root
	 * starting with the supplied test identifier. In case no such source is
	 * found, it falls back to using the parent's
	 * {@linkplain TestIdentifier#getLegacyReportingName legacy reporting name}.
	 *
	 * @param testPlan the test plan that contains the {@code TestIdentifier};
	 *            never {@code null}
	 * @param testIdentifier the identifier to determine the class name for;
	 *            never {@code null}
	 * @see TestIdentifier#getLegacyReportingName
	 */
	public static String getClassName(TestPlan testPlan, TestIdentifier testIdentifier) {
		Preconditions.notNull(testPlan, "testPlan must not be null");
		Preconditions.notNull(testIdentifier, "testIdentifier must not be null");
		for (TestIdentifier current = testIdentifier; current != null; current = getParent(testPlan, current)) {
			ClassSource source = getClassSource(current);
			if (source != null) {
				return source.getClassName();
			}
		}
		return getParentLegacyReportingName(testPlan, testIdentifier);
	}

	private static TestIdentifier getParent(TestPlan testPlan, TestIdentifier testIdentifier) {
		return testPlan.getParent(testIdentifier)
			.orElse(null);
	}

	private static ClassSource getClassSource(TestIdentifier current) {
		// @formatter:off
		return current.getSource()
				.filter(ClassSource.class::isInstance)
				.map(ClassSource.class::cast)
				.orElse(null);
		// @formatter:on
	}

	private static String getParentLegacyReportingName(TestPlan testPlan, TestIdentifier testIdentifier) {
		// @formatter:off
		return testPlan.getParent(testIdentifier)
				.map(TestIdentifier::getLegacyReportingName)
				.orElse("<unrooted>");
		// @formatter:on
	}
}
