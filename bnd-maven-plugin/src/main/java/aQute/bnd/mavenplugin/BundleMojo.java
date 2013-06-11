package aQute.bnd.mavenplugin;

import static aQute.bnd.mavenplugin.Utils.getWorkspace;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

@Mojo(name = "bundle")
public class BundleMojo extends AbstractMojo {

	@Component
	protected MavenSession session;

	@Parameter(property = "supportedProjectTypes")
	protected List<String> supportedProjectTypes = Arrays.asList(new String[] {
			"jar", "bundle" });

	@Component
	protected MavenProject project;

	@Component
	private MavenProjectHelper m_projectHelper;

	@Component
	private ArtifactHandlerManager m_artifactHandlerManager;

	static Pattern JAR_FILE_NAME_P = Pattern.compile("([-a-zA-Z0-9._]+)\\.jar");

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File[] files = null;

		try {
			Workspace workspace = getWorkspace(new File(
					session.getExecutionRootDirectory()));
			Project workspaceProject = retrieveProject(workspace);
			try {

				project.setVersion(workspaceProject.getBundleVersion());
				files = buildProject(workspaceProject);

				if (files == null) {
					getLog().error(workspaceProject.getErrors().toString());
					return;
				}
				if (files.length == 1) {
					Artifact extra = new DefaultArtifact(project.getGroupId(),
							project.getArtifactId(), project.getVersion(),
							"compile", "jar", null,
							m_artifactHandlerManager.getArtifactHandler("jar"));
					extra.setFile(files[0]);
					project.setArtifact(extra);
				} else {
					String uberbsn = project.getArtifactId();

					for (int i = 0; i < files.length; i++) {
						String bsn = files[i].getName();
						Matcher m = JAR_FILE_NAME_P.matcher(bsn);
						if (m.matches()) {
							bsn = m.group(1);
							if (bsn.startsWith(uberbsn)
									&& uberbsn.length() < bsn.length())
								bsn = bsn.substring(uberbsn.length() + 1);
						}

						Artifact extra = new DefaultArtifact(
								project.getGroupId(), project.getArtifactId(),
								project.getVersion(), "compile", "jar", bsn,
								m_artifactHandlerManager
										.getArtifactHandler("jar"));
						extra.setFile(files[i]);
						project.addAttachedArtifact(extra);
					}
				}
			} finally {
				workspace.close();
			}

		} catch (Exception e) {
			throw new MojoExecutionException("Problem building the project.", e);
		}
	}

	private Project retrieveProject(Workspace workspace) throws Exception,
			MojoExecutionException {
		Project workspaceProject = workspace
				.getProject(project.getArtifactId());
		if (project == null)
			throw new MojoExecutionException(
					"Something is broken with your workspace. Cannot find "
							+ project.getArtifactId()
							+ "(from pom.xml) in BND Workspace.");
		return workspaceProject;
	}

	private File[] buildProject(Project workspaceProject) throws Exception {
		File[] files = workspaceProject.build();
		List<String> errors = workspaceProject.getErrors();
		for (String error : errors) {
			getLog().error(error);
		}
		if (errors.size() > 0) {
			throw new MojoExecutionException("There are build errors!.");
		}
		return files;
	}

}
