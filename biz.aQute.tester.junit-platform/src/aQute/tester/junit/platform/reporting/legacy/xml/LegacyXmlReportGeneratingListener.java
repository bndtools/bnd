/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package aQute.tester.junit.platform.reporting.legacy.xml;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * {@code LegacyXmlReportGeneratingListener} is a {@link TestExecutionListener}
 * that generates a separate XML report for each {@linkplain TestPlan#getRoots()
 * root} in the {@link TestPlan}.
 * <p>
 * Note that the generated XML format is compatible with the <em>legacy</em> de
 * facto standard for JUnit 4 based test reports that was made popular by the
 * Ant build system. This code was adapted from the original JUnit 5
 * implementation.
 * <p>
 * Originally copied from JUnit's
 * <tt>org.junit.platform:junit-platform-reporting:1.5.0</tt> and repackaged to
 * avoid name collisions. Made some other alterations to support generating one
 * test file per bundle rather than one per test run, and dropped the API
 * guardian usage to avoid the additional dependency.
 *
 * @since 1.4
 * @see org.junit.platform.launcher.listeners.LoggingListener
 * @see org.junit.platform.launcher.listeners.SummaryGeneratingListener
 */
public class LegacyXmlReportGeneratingListener implements TestExecutionListener {

	private final Path			reportsDir;
	private final PrintWriter	out;
	private final Clock			clock;

	private XmlReportData		reportData;

	public LegacyXmlReportGeneratingListener(Path reportsDir, PrintWriter out) {
		this(reportsDir, out, Clock.systemDefaultZone());
	}

	// For tests only
	LegacyXmlReportGeneratingListener(String reportsDir, PrintWriter out, Clock clock) {
		this(Paths.get(reportsDir), out, clock);
	}

	private LegacyXmlReportGeneratingListener(Path reportsDir, PrintWriter out, Clock clock) {
		this.reportsDir = reportsDir;
		this.out = out;
		this.clock = clock;
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.reportData = new XmlReportData(testPlan, clock);
		try {
			Files.createDirectories(this.reportsDir);
		} catch (IOException e) {
			printException("Could not create reports directory: " + this.reportsDir, e);
		}
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		this.reportData = null;
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		this.reportData.markSkipped(testIdentifier, reason);
		writeXmlReportInCaseOfBundle(testIdentifier);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		this.reportData.markStarted(testIdentifier);
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		this.reportData.addReportEntry(testIdentifier, entry);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
		this.reportData.markFinished(testIdentifier, result);
		writeXmlReportInCaseOfBundle(testIdentifier);
	}

	private void writeXmlReportInCaseOfBundle(TestIdentifier testIdentifier) {
		List<Segment> segments = UniqueId.parse(testIdentifier.getUniqueId())
			.getSegments();
		Segment last = segments.get(segments.size() - 1);
		if (last.getType()
			.equals("bundle")) {
			String bundleDesc = last.getValue()
				.replace(';', '-');
			writeXmlReportSafely(testIdentifier, bundleDesc);
		}
	}

	private void writeXmlReportSafely(TestIdentifier testIdentifier, String rootName) {
		Path xmlFile = this.reportsDir.resolve("TEST-" + rootName + ".xml");
		try (Writer fileWriter = Files.newBufferedWriter(xmlFile)) {
			new XmlReportWriter(this.reportData).writeXmlReport(testIdentifier, fileWriter);
		} catch (XMLStreamException | IOException e) {
			printException("Could not write XML report: " + xmlFile, e);
		}
	}

	private void printException(String message, Exception exception) {
		out.println(message);
		exception.printStackTrace(out);
	}
}
