package aQute.bnd.mavenplugin;

import java.io.*;
import java.util.*;

import org.apache.maven.*;
import org.apache.maven.execution.*;
import org.apache.maven.model.*;
import org.apache.maven.project.*;
import org.codehaus.plexus.component.annotations.*;
import org.codehaus.plexus.logging.*;

import aQute.bnd.build.*;

/**
 * http://brettporter.wordpress.com/2010/10/05/creating-a-custom-build-extension
 * -for-maven-3-0/
 * 
 */

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "beer")
public class LCParticipant extends AbstractMavenLifecycleParticipant {

	@Requirement
	Logger logger;

	@Override
	public void afterSessionStart(MavenSession session)
			throws MavenExecutionException {
		System.out.println("AFTER SESSION START");
		System.out.println(session.getProjects());
		session.getProjects().remove(3);
		System.out.println(session.getProjects());
	}

	@Override
	public void afterProjectsRead(MavenSession session)
			throws MavenExecutionException {
		try {
			System.out.println("AFTER PROJECTS READ");
			Workspace ws = Utils.getWorkspace( new File(session.getExecutionRootDirectory()));
			System.out.println("Workspace " + ws.getBase());
			
			Map<MavenProject, Project> index = new HashMap<MavenProject, Project>();
			Map<Project, MavenProject> inverted = new HashMap<Project, MavenProject>();

			for (MavenProject mp : session.getProjects()) {
				logger.info("+ " + mp.getBasedir());
				Project bp = ws.getProject(mp.getArtifactId());
				if (bp == null) {
					logger.info("Cannot find a bnd project for " + mp);
				} else {
					index.put(mp, bp);
					inverted.put(bp, mp);
				}
			}

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
						logger.error("Dependency missing from " + e.getValue()
								+ " to " + bp);
					}
				}
			}

			System.out.println(session.getProjects());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
