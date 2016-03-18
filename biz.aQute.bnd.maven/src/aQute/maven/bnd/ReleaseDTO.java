package aQute.maven.bnd;

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.util.dto.DTO;

public class ReleaseDTO extends DTO {
	public enum ReleaseType {
		LOCAL, REMOTE;
	}

	public static class JavadocDTO extends DTO {
		public String				path;
		public Map<String,String>	options	= new HashMap<>();
	}

	public static class PomDTO extends DTO {
		public String path;
	}

	public static class SourceDTO extends DTO {
		public String path;
	}

	public ReleaseType		type;
	public JavadocDTO		javadoc;
	public PomDTO			pom;
	public SourceDTO		source;
}
