package aQute.tester.test.utils;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.framework.Bundle;

import aQute.lib.collections.MultiMap;

public class TestRunData {
	Map<String, TestEntry>		nameMap			= new HashMap<>();
	Map<String, TestEntry>		idMap			= new HashMap<>();
	MultiMap<String, String>	parentMap		= new MultiMap<>();
	MultiMap<String, String>	executionMap	= new MultiMap<>();
	Map<String, TestFailure>	failureMap		= new HashMap<>();
	MultiMap<String, String>	startNameMap	= new MultiMap<>();
	MultiMap<String, String>	endNameMap		= new MultiMap<>();
	Deque<String>				started			= new ArrayDeque<>();
	int							testCount;
	long						actualRunTime;
	long						reportedRunTime;

	public static String nameOf(Bundle bundle) {
		final Optional<String> bundleName = Optional.ofNullable(bundle.getHeaders()
			.get(aQute.bnd.osgi.Constants.BUNDLE_NAME));
		return "[" + bundle.getBundleId() + "] " + bundleName.orElse(bundle.getSymbolicName()) + ';'
			+ bundle.getVersion();
	}

	public TestFailure getFailure(String id) {
		return failureMap.get(id);
	}

	public TestFailure getFailureByName(String name) {
		TestEntry e = nameMap.get(name);
		if (e == null) {
			return null;
		}
		return failureMap.get(e.testId);
	}

	public TestEntry getById(String id) {
		return idMap.get(id);
	}

	public String getStartName(String id) {
		List<String> startName = startNameMap.get(id);
		return startName != null ? startName.get(0) : null;
	}

	public String getEndName(String id) {
		List<String> endName = endNameMap.get(id);
		return endName != null ? endName.get(0) : null;
	}

	public Map<String, TestEntry> getTests() {
		return idMap;
	}

	public int getTestCount() {
		return testCount;
	}

	public List<String> getExecutedChildrenOf(String id) {
		return executionMap.get(id);
	}

	public List<String> getChildrenOf(String id) {
		List<String> children = parentMap.get(id);
		return children == null ? Collections.emptyList() : children;
	}

	public void setActualRunTime(long runTime) {
		actualRunTime = runTime;
	}

	public long getActualRunTime() {
		return actualRunTime;
	}

	public Map<String, TestEntry> getNameMap() {
		return nameMap;
	}

	public TestEntry getTest(Class<?> testClass, String testMethod) {
		TestEntry e = nameMap.get(nameOf(testClass));
		if (e == null) {
			return null;
		}
		List<String> children = getChildrenOf(e.testId);
		return children == null ? null
			: children.stream()
				.filter(x -> getById(x).testName.equals(nameOf(testClass, testMethod)))
				.findFirst()
				.map(this::getById)
				.orElse(null);
	}

	public static String nameOf(Class<?> testClass, String method) {
		return method + '(' + testClass.getName() + ')';
	}

	public static String nameOf(Class<?> testClass) {
		return testClass.getName();
	}

	public TestRunData() {
		super();
	}

	public Map<String, TestFailure> getFailureMap() {
		return failureMap;
	}

	public MultiMap<String, String> getStartNameMap() {
		return startNameMap;
	}

	public MultiMap<String, String> getEndNameMap() {
		return endNameMap;
	}

}
