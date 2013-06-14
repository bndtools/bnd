package aQute.bnd.mavenplugin;

import java.io.*;
import java.util.*;

import org.apache.maven.*;
import org.apache.maven.execution.*;
import org.apache.maven.model.*;
import org.apache.maven.plugin.*;
import org.apache.maven.project.*;
import org.codehaus.plexus.component.annotations.*;
import org.codehaus.plexus.logging.*;

import aQute.bnd.build.*;

/**
 * Since we have the project ordering information in bnd, we can reorder the
 * list of projects. However, that requires us to start very early in the
 * process. This is done through a AbstractMavenLifecycleParticipant. After the
 * projects are read we reorder the list according to the bnd deps. We provide
 * warnings for missing projects and of course cycles.
 * 
 * The example that helped me write this:
 * 
 * {@code http://brettporter.wordpress.com/2010/10/05/creating-a-custom-build-extension-for-maven-3-0/ }
 * 
 */

@Component(role = AbstractMavenLifecycleParticipant.class)
public class InitializeForBnd extends AbstractMavenLifecycleParticipant {

	@Requirement
	private Logger logger;
	
	@Requirement
	private BndWorkspace bndWorkspace;

	@Override
	public void afterProjectsRead(MavenSession session)
			throws MavenExecutionException {
		try {
			Workspace workspace = bndWorkspace.getWorkspace(session);

			// Build an index and an inverted list of the projects

			Map<MavenProject, Project> index = new HashMap<MavenProject, Project>();
			Map<Project, MavenProject> inverted = new HashMap<Project, MavenProject>();

			for (MavenProject mp : session.getProjects()) {
				logger.info("+ " + mp.getBasedir());
				Project bp = workspace.getProject(mp.getArtifactId());
				if (bp == null) {
					logger.warn("[bnd] cannot find a bnd project for " + mp + " " + mp.isExecutionRoot());
				} else {
					index.put(mp, bp);
					inverted.put(bp, mp);
				}
			}

			// Go through the list of the maven projects and create
			// a dependency on any project in the current build that we can find
			// maven will then automatically order them correctly
			// (the reactor order I think it is called)

			for (Map.Entry<MavenProject, Project> e : index.entrySet()) {
				for (Project bp : e.getValue().getDependson()) {
					MavenProject mp = inverted.get(bp);
					if (mp != null) {
						Dependency dp = new Dependency();
						dp.setArtifactId(mp.getArtifactId());
						dp.setGroupId(mp.getGroupId());
						dp.setVersion(mp.getVersion());
						e.getKey().getModel().addDependency(dp);
					} else {
						logger.error("[bnd] dependency missing from "
								+ e.getValue() + " to " + bp);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Hmm, seems never to get called
	 */
	@Override
	public void afterSessionStart(MavenSession session)
			throws MavenExecutionException {
	}
}
