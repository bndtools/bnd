package aQute.maven.nexus.provider;

import java.net.URI;
import java.util.List;

import aQute.bnd.util.dto.DTO;

/**
 * @formatter:off
 * {
 * 	"data":	[
 * 		{
 * 			"resourceURI":"https://oss.sonatype.org/service/local/repositories/orgosgi-1073/content/org/osgi/osgi.enroute.authenticator.github.provider/","relativePath":"/org/osgi/osgi.enroute.authenticator.github.provider/",
 * 			"text":"osgi.enroute.authenticator.github.provider",
 * 			"leaf":false,
 * 			"lastModified":"2016-06-03 17:05:14.0 UTC",
 * 			"sizeOnDisk":-1
 * 		}
 * 	]
 * }
 * @formatter:on
 *
 */
public class ContentDTO extends DTO {
	public static class ItemDTO extends DTO {
		public URI		resourceURI;
		public String	text;
		public boolean	leaf;
		public long		sizeOnDisk;
	}

	public List<ItemDTO> data;
}
