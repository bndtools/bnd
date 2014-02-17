package aQute.bnd.build;

public class JUnitLauncher extends ProjectLauncher {

	public JUnitLauncher(Project project) throws Exception {
		super(project);
	}

	@Override
	public String getMainTypeName() {
		return "junit.JUnitRunner";
	}

	@Override
	public void update() throws Exception {

	}

	@Override
	public void prepare() throws Exception {

	}

}
