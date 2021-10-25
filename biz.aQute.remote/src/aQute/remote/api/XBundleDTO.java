package aQute.remote.api;

import java.util.List;
import java.util.Map;

import org.osgi.framework.dto.BundleDTO;

public class XBundleDTO extends BundleDTO {

	public String					state;
	public String					location;
	public String					category;
	public int						revisions;
	public boolean					isFragment;
	public long						lastModified;
	public String					documentation;
	public String					vendor;
	public String					description;
	public int						startLevel;
	public List<XPackageDTO>		exportedPackages;
	public List<XPackageDTO>		importedPackages;
	public List<XBundleInfoDTO>		wiredBundlesAsProvider;
	public List<XBundleInfoDTO>		wiredBundlesAsRequirer;
	public List<XServiceInfoDTO>	registeredServices;
	public Map<String, String>		manifestHeaders;
	public List<XServiceInfoDTO>	usedServices;
	public List<XBundleInfoDTO>		hostBundles;
	public List<XBundleInfoDTO>		fragmentsAttached;
	public boolean					isPersistentlyStarted;
	public boolean					isActivationPolicyUsed;

}
