package bndtools.m2e;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.m2e.core.embedder.ArtifactKey;

import aQute.bnd.exceptions.Exceptions;

public class ArtifactKeyHelper {

	public static String getGroupId(ArtifactKey key) {
		return doExtract(key, "getGroupId", "groupId");
	}

	public static String getArtifactId(ArtifactKey key) {
		return doExtract(key, "getArtifactId", "artifactId");
	}

	public static String getVersion(ArtifactKey key) {
		return doExtract(key, "getVersion", "version");
	}

	public static String getClassifier(ArtifactKey key) {
		return doExtract(key, "getClassifier", "classifier");
	}

	private static String doExtract(ArtifactKey key, String getter, String property) {
		Method m;
		try {
			try {
				m = ArtifactKey.class.getMethod(getter);
			} catch (NoSuchMethodException nsme) {
				m = ArtifactKey.class.getMethod(property);
			}
			return (String) m.invoke(key);
		} catch (InvocationTargetException ite) {
			Exceptions.duck(ite.getCause());
		} catch (Exception e) {
			Exceptions.duck(e);
		}
		return null;
	}
}
