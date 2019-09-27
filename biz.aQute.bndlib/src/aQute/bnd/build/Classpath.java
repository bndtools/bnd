package aQute.bnd.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;

public class Classpath {

	public interface ClassVisitor {
		boolean visit(Clazz clazz) throws Exception;
	}

	List<File>			entries	= new ArrayList<>();
	private Reporter	project;
	private String		name;

	public Classpath(Reporter project, String name) {
		this.project = project;
		this.name = name;
	}

	public void add(Collection<Container> testpath) throws Exception {
		for (Container c : Container.flatten(testpath)) {
			if (c.getError() != null) {
				project.error("Adding %s to %s, got error: %s", c, name, c.getError());
			} else {
				entries.add(c.getFile()
					.getAbsoluteFile());
			}
		}
	}

	public List<File> getEntries() {
		return entries;
	}

	/**
	 * Visit each class on the class path.
	 *
	 * @param visitor the visitor
	 */
	public void visit(ClassVisitor visitor) throws Exception {
		try (Analyzer analyzer = new Analyzer()) {
			for (File f : entries) {
				try (Jar jar = new Jar(f)) {
					for (String path : jar.getResources()
						.keySet()) {
						if (path.endsWith(".class")) {
							Resource r = jar.getResource(path);
							Clazz c = new Clazz(analyzer, path, r);
							c.parseClassFile();
							visitor.visit(c);
						}
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return Strings.join(File.pathSeparator, entries);
	}
}
