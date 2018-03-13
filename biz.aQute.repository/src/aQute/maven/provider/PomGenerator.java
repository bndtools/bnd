package aQute.maven.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import aQute.lib.tag.Tag;
import aQute.maven.api.Archive;
import aQute.maven.api.Revision;

/**
 * Generate a POM with the contents of the repository as dependencies
 */
public class PomGenerator {

	private Revision			name;
	private Revision			parent;
	private boolean				dependencyManagement;
	private Collection<Archive>	dependencies;

	public PomGenerator(Collection<Archive> dependencies) {
		this.dependencies = dependencies;
	}

	public PomGenerator name(Revision name) {
		this.name = name;
		return this;
	}

	public PomGenerator parent(Revision name) {
		this.parent = name;
		return this;
	}

	public PomGenerator dependencyManagement(boolean value) {
		this.dependencyManagement = value;
		return this;
	}

	public void out(OutputStream out) throws IOException {
		try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
			PrintWriter pw = new PrintWriter(w);
			pw.println("<?xml version='1.0' encoding='UTF-8'?>");
			Tag tag = build();
			tag.print(1, pw);
		}
	}

	public Tag build() {
		prune();
		Tag project = new Tag("project");
		project.addAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
		project.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		project.addAttribute("xsi:schemaLocation",
			"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");

		new Tag(project, "modelVersion", "4.0.0");

		if (parent != null) {
			Tag parent = new Tag(project, "parent");
			gav(parent, this.parent);
		}

		gav(project, name);

		Tag depType;
		if (dependencyManagement) {
			depType = new Tag(project, "dependencyManagement");
		} else
			depType = project;

		Tag dependencies = new Tag(depType, "dependencies");

		for (Archive dep : this.dependencies) {
			Tag dependency = new Tag(dependencies, "dependency");
			gav(dependency, dep.revision);
			if (dep.hasClassifier())
				new Tag(dependency, "classifier", dep.classifier);
			if (dep.hasExtension())
				new Tag(dependency, "type", dep.extension);

			if (!dependencyManagement)
				new Tag(dependency, "scope", "runtime");
		}

		return project;
	}

	private void prune() {
		TreeSet<Archive> s = new TreeSet<>(dependencies);
		Archive prev = null;

		for (Iterator<Archive> i = s.iterator(); i.hasNext();) {
			Archive a = i.next();
			boolean sameProgram = prev != null && prev.revision.program.equals(a.revision.program);
			if (sameProgram) {
				System.out.println("Skipping " + a);
				i.remove();
			} else
				prev = a;
		}
		dependencies = s;
	}

	private void gav(Tag parent, Revision name) {
		new Tag(parent, "groupId", name.group);
		new Tag(parent, "artifactId", name.artifact);
		new Tag(parent, "version", name.version);
	}

}
