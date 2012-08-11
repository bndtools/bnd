package aQute.bnd.build;

import java.io.*;
import java.util.*;

import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;

public class ProjectBuilder extends Builder {
	private final DiffPluginImpl	differ	= new DiffPluginImpl();
	Project							project;
	boolean							initialized;

	public ProjectBuilder(Project project) {
		super(project);
		this.project = project;
	}

	public ProjectBuilder(ProjectBuilder builder) {
		super(builder);
		this.project = builder.project;
	}

	@Override
	public long lastModified() {
		return Math.max(project.lastModified(), super.lastModified());
	}

	/**
	 * We put our project and our workspace on the macro path.
	 */
	@Override
	protected Object[] getMacroDomains() {
		return new Object[] {
				project, project.getWorkspace()
		};
	}

	@Override
	public Builder getSubBuilder() throws Exception {
		return project.getBuilder(this);
	}

	public Project getProject() {
		return project;
	}

	@Override
	public void init() {
		try {
			if (!initialized) {
				initialized = true;
				for (Container file : project.getClasspath()) {
					addClasspath(file.getFile());
				}

				for (Container file : project.getBuildpath()) {
					addClasspath(file.getFile());
				}

				for (Container file : project.getBootclasspath()) {
					addClasspath(file.getFile());
				}

				for (File file : project.getAllsourcepath()) {
					addSourcepath(file);
				}

			}
		}
		catch (Exception e) {
			msgs.Unexpected_Error_("ProjectBuilder init", e);
		}
	}

	@Override
	public List<Jar> getClasspath() {
		init();
		return super.getClasspath();
	}

	@Override
	protected void changedFile(File f) {
		project.getWorkspace().changedFile(f);
	}

	/**
	 * Compare this builder's JAR with a baseline
	 * 
	 * @throws Exception
	 */
	@Override
	protected void doBaseline(Jar dot) throws Exception {

		String s = getProperty(Constants.BASELINE);
		if (s == null || s.trim().length() == 0)
			return;

		trace("baseline %s", s);

		Baseline baseline = new Baseline(this, differ);

		Collection<Container> bundles = project.getBundles(Strategy.LOWEST, s);
		for (Container c : bundles) {
			
			if (c.getError() != null || c.getFile() == null) {
				error("Erroneous baseline bundle %s", c);
				continue;
			}

			Jar jar = new Jar(c.getFile());
			try {
				Set<Info> infos = baseline.baseline(dot, jar, null);
				for (Info info : infos) {
					if (info.mismatch) {
						error("%s %-50s %-10s %-10s %-10s %-10s %-10s\n", info.mismatch ? '*' : ' ', info.packageName,
								info.packageDiff.getDelta(), info.newerVersion, info.olderVersion, info.suggestedVersion,
								info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
					}
				}				
			}
			finally {
				jar.close();
			}
		}

	}

}
