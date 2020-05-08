package aQute.bnd.service.generate;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;

@ProviderType
public class BuildContext extends Processor {

	private final Project		project;
	private final List<String>	arguments;
	private final InputStream	stdin;
	private final OutputStream	stdout;
	private final OutputStream	stderr;

	public BuildContext(Project project, Map<String, String> localProperties, List<String> arguments, InputStream stdin,
		OutputStream stdout, OutputStream stderr) {
		super(project);
		this.stdin = stdin;
		this.stdout = stdout;
		this.stderr = stderr;
		this.setBase(project.getBase());
		use(project);
		this.project = project;
		this.arguments = arguments;
		for (Map.Entry<String, String> e : localProperties.entrySet()) {
			this.set(e.getKey(), e.getValue());
		}

	}

	public Project getProject() {
		return project;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public InputStream getStdin() {
		return stdin;
	}

	public OutputStream getStdout() {
		return stdout;
	}

	public OutputStream getStderr() {
		return stderr;
	}
}
