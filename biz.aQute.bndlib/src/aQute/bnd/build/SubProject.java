package aQute.bnd.build;

import java.io.File;

import aQute.bnd.osgi.Processor;
import aQute.lib.strings.Strings;

/**
 * Models a sub project when the `-sub` instruction is used.
 */
public class SubProject extends Processor {

	final Project	project;
	final String	name;

	SubProject(Project project, File properties) {
		super(project);
		assert properties != null && properties.isFile();
		this.project = project;
		String[] parts = Strings.extension(properties.getName());
		if (parts == null)
			name = properties.getName();
		else
			name = parts[0];

		setBase(project.getBase());
		this.setProperties(properties);
		use(project);
	}

	@Override
	public String toString() {
		return project + "." + getName();
	}

	public Project getProject() {
		return project;
	}

	public String getName() {
		return name;
	}

	public ProjectBuilder getProjectBuilder() throws Exception {
		ProjectBuilder builder = new ProjectBuilder(project);
		builder.setProperties(getPropertiesFile());
		builder.use(this);
		addClose(this);
		return builder;
	}
}
