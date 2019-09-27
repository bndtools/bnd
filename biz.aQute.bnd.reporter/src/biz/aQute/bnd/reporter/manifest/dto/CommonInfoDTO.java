package biz.aQute.bnd.reporter.manifest.dto;

import java.util.List;

import org.osgi.dto.DTO;

public class CommonInfoDTO extends DTO {

	public String				name;
	public String				description;
	public VersionDTO			version;
	public List<IconDTO>		icons;
	public String				docURL;
	public String				updateLocation;
	public List<LicenseDTO>		licenses;
	public List<DeveloperDTO>	developers;
	public ScmDTO				scm;
	public String				copyright;
	public String				vendor;
	public ContactAddressDTO	contactAddress;
}
