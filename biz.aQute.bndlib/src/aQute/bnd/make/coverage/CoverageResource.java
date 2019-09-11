package aQute.bnd.make.coverage;

import static aQute.bnd.make.coverage.Coverage.getCrossRef;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.WriteResource;
import aQute.lib.tag.Tag;

/**
 * Creates an XML Coverage report. This class can be used as a resource so the
 * report is created only when the JAR is written.
 */
public class CoverageResource extends WriteResource {
	Collection<Clazz>	testsuite;
	Collection<Clazz>	service;

	public CoverageResource(Collection<Clazz> testsuite, Collection<Clazz> service) {
		this.testsuite = testsuite;
		this.service = service;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		try {
			Map<MethodDef, List<MethodDef>> table = getCrossRef(testsuite, service);
			Tag coverage = toTag(table);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, Constants.DEFAULT_CHARSET));
			try {
				coverage.print(0, pw);
			} finally {
				pw.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Tag toTag(Map<MethodDef, List<MethodDef>> catalog) {
		Tag coverage = new Tag("coverage");
		String currentClass = null;
		Tag classTag = null;

		for (Map.Entry<MethodDef, List<MethodDef>> m : catalog.entrySet()) {
			String className = m.getKey()
				.getContainingClass()
				.getFQN();
			if (!className.equals(currentClass)) {
				classTag = new Tag("class");
				classTag.addAttribute("name", className);
				classTag.addAttribute("package", Descriptors.getPackage(className));
				classTag.addAttribute("short", Descriptors.getShortName(className));
				coverage.addContent(classTag);
				currentClass = className;
			}
			Tag method = doMethod(new Tag("method"), m.getKey());
			if (classTag != null)
				classTag.addContent(method);
			for (MethodDef r : m.getValue()) {
				Tag ref = doMethod(new Tag("ref"), r);
				method.addContent(ref);
			}
		}
		return coverage;
	}

	private static Tag doMethod(Tag tag, MethodDef method) {
		tag.addAttribute("pretty", method.toString());
		if (method.isPublic())
			tag.addAttribute("public", true);
		if (method.isStatic())
			tag.addAttribute("static", true);
		if (method.isProtected())
			tag.addAttribute("protected", true);
		if (method.isInterface())
			tag.addAttribute("interface", true);
		tag.addAttribute("constructor", method.isConstructor());
		if (!method.isConstructor())
			tag.addAttribute("name", method.getName());
		tag.addAttribute("descriptor", method.descriptor());
		return tag;
	}
}
