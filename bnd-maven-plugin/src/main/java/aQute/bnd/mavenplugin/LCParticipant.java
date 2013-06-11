package aQute.bnd.mavenplugin;

import org.apache.maven.*;
import org.apache.maven.execution.*;
import org.codehaus.plexus.component.annotations.*;


@Component(role = AbstractMavenLifecycleParticipant.class, hint = "beer")
public class LCParticipant extends AbstractMavenLifecycleParticipant {

	@Override
	public void afterSessionStart(MavenSession session)
			throws MavenExecutionException {
		System.out.println("AFTER SESSION START");
	}

	@Override
	public void afterProjectsRead(MavenSession session)
			throws MavenExecutionException {
		System.out.println("AFTER PROJECTS READ");
	}

}
