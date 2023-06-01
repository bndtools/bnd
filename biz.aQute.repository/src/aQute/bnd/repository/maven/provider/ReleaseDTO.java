package aQute.bnd.repository.maven.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.util.dto.DTO;

public class ReleaseDTO extends DTO {
	public enum ReleaseType {
		LOCAL,
		REMOTE;
	}

	public enum JavadocPackages {
		ALL,
		EXPORT;
	}

	public static class JavadocDTO extends DTO {
		public String				path;
		public JavadocPackages		packages	= JavadocPackages.EXPORT;
		public Map<String, String>	options		= new HashMap<>();
		public boolean				force;
	}

	public static class PomDTO extends DTO {
		public String path;
	}

	public static class SourceDTO extends DTO {
		public String				path;
		public Map<String, String>	options	= new HashMap<>();
		public boolean				force;
	}

	public static class ExtraDTO extends DTO {
		public String				path;
		public String				clazz;
		public Map<String, String>	options	= new HashMap<>();
	}

	public ReleaseType		type		= ReleaseType.LOCAL;
	public JavadocDTO		javadoc		= new JavadocDTO();
	public PomDTO			pom			= new PomDTO();
	public SourceDTO		sources		= new SourceDTO();
	public List<ExtraDTO>	extra		= new ArrayList<>();
	public long				snapshot	= -1;
	public String			passphrase;
}
