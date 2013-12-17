package aQute.bnd.mavenplugin;


import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.maven.artifact.*;
import org.apache.maven.artifact.handler.manager.*;
import org.apache.maven.execution.*;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.*;

import aQute.bnd.build.*;
import aQute.service.reporter.*;

/**
 * The mojo responsible for packaging the bundle.
 */
@Mojo(name = "bundle")
public class BundlePackager extends AbstractMojo {

	@Component
	protected MavenSession session;

	@Parameter(property = "supportedProjectTypes")
	protected List<String> supportedProjectTypes = Arrays.asList(new String[] {
			"jar", "bundle" });

	@Component
	protected MavenProject mavenProject;

	@Component
	private ArtifactHandlerManager m_artifactHandlerManager;
	
	@Component
	private BndWorkspace bndWorkspace;

	static Pattern JAR_FILE_NAME_P = Pattern.compile("([-a-zA-Z0-9._]+)\\.jar");

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File[] files = null;

		try {
			Workspace workspace = bndWorkspace.getWorkspace(session);
			Project bndProject = workspace.getProject(mavenProject.getArtifactId());
			if ( bndProject == null)
				throw new MojoExecutionException("Cannot find the bnd project " + mavenProject.getArtifactId() + " in workspace " + workspace );
				
			files = bndProject.build();
			
			// Report any errors and warnings
			
			report("Workspace", workspace);
			report("Project", bndProject);
			
			// We stop if we ran into an error
			
			if ( !workspace.isOk() || !bndProject.isOk()) {
				throw new MojoExecutionException("Project " + bndProject + " has failed" );
			}

			//
			// If we get 1 file, we treat this as the main artifact of this
			// maven project. Otherwise, we treat the multiple output files
			// as classifiers
			//
			
			if (files.length == 1) {
				Artifact extra = new DefaultArtifact(mavenProject.getGroupId(),
						mavenProject.getArtifactId(), mavenProject.getVersion(),
						"compile", "jar", null,
						m_artifactHandlerManager.getArtifactHandler("jar"));
				extra.setFile(files[0]);
				mavenProject.setArtifact(extra);
			} else {
				String uberbsn = mavenProject.getArtifactId();

				for (int i = 0; i < files.length; i++) {
					String bsn = files[i].getName();
					Matcher m = JAR_FILE_NAME_P.matcher(bsn);
					if (m.matches()) {
						bsn = m.group(1);
						if (bsn.startsWith(uberbsn)
								&& uberbsn.length() < bsn.length())
							bsn = bsn.substring(uberbsn.length() + 1);
					}

					Artifact extra = new DefaultArtifact(mavenProject.getGroupId(),
							mavenProject.getArtifactId(), mavenProject.getVersion(),
							"compile", "jar", bsn,
							m_artifactHandlerManager.getArtifactHandler("jar"));
					extra.setFile(files[i]);
					mavenProject.addAttachedArtifact(extra);
				}
			}

		} catch (Exception e) {
			throw new MojoExecutionException("Problem building the project.", e);
		}
	}

	/**
	 * Just report the errors and warnings
	 * 
	 * @param title
	 * @param report
	 */
	private void report(String title, Report report ) {
		if ( !report.getErrors().isEmpty()) {
			getLog().error( title + " " + report );
			for ( int i=0; i<report.getErrors().size(); i++) {
				getLog().error( "[E"+i+"] " +report.getErrors().get(i));
			}
			for ( int i=0; i<report.getWarnings().size(); i++) {
				getLog().error( "[W"+i+"] " +report.getWarnings().get(i));
			}
		}
	}



}
