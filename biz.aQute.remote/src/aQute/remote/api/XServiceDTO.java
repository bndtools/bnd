package aQute.remote.api;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class XServiceDTO extends DTO {

	public long					id;
	public List<String>			types;
	public long					bundleId;
	public Map<String, String>	properties;
	public List<XBundleInfoDTO>	usingBundles;
	public String				registeringBundle;

}
