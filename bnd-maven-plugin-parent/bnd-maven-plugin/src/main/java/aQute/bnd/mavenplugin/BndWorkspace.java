package aQute.bnd.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;

import aQute.bnd.build.Workspace;

/**
 * A Plexus component that provides MavenSession to BND Workspace mapping.
 * 
 * @author rafal.krzewski@caltha.pl
 */
@Component(role = BndWorkspace.class)
public class BndWorkspace {

	private Map<MavenSession, Workspace>	workspaces	= new WeakHashMap<MavenSession, Workspace>();

	public Workspace getWorkspace(MavenSession session) throws MojoFailureException {

		File executionRoot = new File(session.getExecutionRootDirectory());

		Workspace workspace = workspaces.get(session);

		if (workspace != null)
			return workspace;

		try {
			File currentFolder = new File("cnf").getCanonicalFile();
			if (currentFolder.exists() && currentFolder.isDirectory())
				workspace = new Workspace(currentFolder.getParentFile());
			else {
				File sessionRoot = new File(executionRoot, "/cnf").getCanonicalFile();
				if (sessionRoot.exists() && sessionRoot.isDirectory())
					workspace = new Workspace(sessionRoot.getParentFile());
				else {
					File upFolder = new File("../cnf").getCanonicalFile();
					if (upFolder.exists() && upFolder.isDirectory())
						workspace = new Workspace(upFolder.getParentFile());
				}
			}
		} catch (IOException e) {
			throw new MojoFailureException("Failed to access workspace folder", e);
		} catch (Exception e) {
			throw new MojoFailureException("Failed to initialize BND workspace", e);
		}

		if (workspace != null) {
			workspaces.put(session, workspace);
			return workspace;
		}
		throw new MojoFailureException("No workspace folder found!");
	}

}
