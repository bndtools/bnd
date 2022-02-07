package org.bndtools.test.assertj.eclipse.jdt.core.compiler.iproblem;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.compiler.IProblem;
import org.osgi.test.common.exceptions.Exceptions;

public final class IProblemMap {
	final static Map<Integer, String> PROBLEM_MAP;

	static {
		Field[] problemTypes = IProblem.class.getFields();
		Map<Integer, String> problemMap = new HashMap<>(problemTypes.length * 2 - 1);
		try {
			for (Field problemType : problemTypes) {
				problemMap.put(problemType.getInt(null), problemType.getName());
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
		PROBLEM_MAP = Collections.unmodifiableMap(problemMap);
	}

	public static String getProblemDescription(int problemId) {
		final String desc = PROBLEM_MAP.getOrDefault(problemId, "");
		return "Pb(" + (problemId & IProblem.IgnoreCategoriesMask) + ")"
			+ (desc == null || desc.isEmpty() ? "" : " " + desc);
	}
}
