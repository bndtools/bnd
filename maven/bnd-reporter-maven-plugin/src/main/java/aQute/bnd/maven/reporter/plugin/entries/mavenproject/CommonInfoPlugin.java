package aQute.bnd.maven.reporter.plugin.entries.mavenproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.maven.project.MavenProject;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.maven.reporter.plugin.MavenProjectWrapper;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.bnd.version.Version;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.manifest.dto.CommonInfoDTO;
import biz.aQute.bnd.reporter.manifest.dto.ContactAddressDTO;
import biz.aQute.bnd.reporter.manifest.dto.DeveloperDTO;
import biz.aQute.bnd.reporter.manifest.dto.LicenseDTO;
import biz.aQute.bnd.reporter.manifest.dto.ScmDTO;
import biz.aQute.bnd.reporter.manifest.dto.VersionDTO;

/**
 * This plugins extract common info from a maven project pom.
 */
@BndPlugin(name = "entry." + EntryNamesReference.COMMON_INFO)
public class CommonInfoPlugin implements ReportEntryPlugin<MavenProjectWrapper>, Plugin {

	private final Map<String, String> _properties = new HashMap<>();

	public CommonInfoPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.COMMON_INFO);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, MavenProjectWrapper.class.getCanonicalName());
	}

	@Override
	public void setReporter(final Reporter processor) {
		// Nothing
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public CommonInfoDTO extract(final MavenProjectWrapper obj, final Locale locale) throws Exception {
		MavenProject project = obj.getProject();
		final CommonInfoDTO commonInfo = new CommonInfoDTO();

		commonInfo.contactAddress = extractContactAddress(project);
		commonInfo.description = extractDescription(project);
		commonInfo.developers = extractDevelopers(project);
		commonInfo.docURL = extractDocURL(project);
		commonInfo.licenses = extractLicenses(project);
		commonInfo.name = extractName(project);
		commonInfo.scm = extractScm(project);
		commonInfo.updateLocation = extractUpdateLocation(project);
		commonInfo.vendor = extractVendor(project);
		commonInfo.version = extractVersion(project);

		return commonInfo;
	}

	private ContactAddressDTO extractContactAddress(MavenProject project) {
		if (project.getOrganization() != null && project.getOrganization()
			.getUrl() != null) {
			ContactAddressDTO contact = new ContactAddressDTO();
			contact.address = project.getOrganization()
				.getUrl();
			contact.type = "url";
			return contact;
		} else {
			return null;
		}
	}

	private String extractDescription(MavenProject project) {
		return project.getDescription() != null && !project.getDescription()
			.isEmpty() ? project.getDescription() : null;
	}

	private List<DeveloperDTO> extractDevelopers(MavenProject project) {
		List<DeveloperDTO> developers = new LinkedList<>();
		if (project.getDevelopers() != null) {
			project.getDevelopers()
				.forEach(d -> {
					DeveloperDTO developer = new DeveloperDTO();
					developer.identifier = d.getId();
					developer.email = d.getEmail();
					developer.name = d.getName();
					developer.organization = d.getOrganization();
					developer.organizationUrl = d.getOrganizationUrl();
					if (d.getRoles() != null) {
						developer.roles = new ArrayList<>(d.getRoles());
					}
					if (d.getTimezone() != null) {
						if (isInteger(d.getTimezone())) {
							developer.timezone = Integer.valueOf(d.getTimezone());
						} else {
							developer.timezone = (int) TimeUnit.HOURS.convert(TimeZone.getTimeZone(d.getTimezone())
								.getRawOffset(), TimeUnit.MILLISECONDS);
						}
					}
					developers.add(developer);
				});
		}
		return !developers.isEmpty() ? developers : null;
	}

	private String extractDocURL(MavenProject project) {
		return project.getUrl() != null && !project.getUrl()
			.isEmpty() ? project.getUrl() : null;
	}

	private List<LicenseDTO> extractLicenses(MavenProject project) {
		List<LicenseDTO> licenses = new LinkedList<>();
		if (project.getLicenses() != null) {
			project.getLicenses()
				.forEach(l -> {
					LicenseDTO license = new LicenseDTO();
					license.description = l.getComments();
					license.name = l.getName();
					license.link = l.getUrl();
					licenses.add(license);
				});
		}
		return !licenses.isEmpty() ? licenses : null;
	}

	private String extractName(MavenProject project) {
		return project.getName() != null && !project.getName()
			.isEmpty() ? project.getName() : null;
	}

	private ScmDTO extractScm(MavenProject project) {
		if (project.getScm() != null) {
			ScmDTO scm = new ScmDTO();
			scm.connection = project.getScm()
				.getConnection();
			scm.developerConnection = project.getScm()
				.getDeveloperConnection();
			scm.url = project.getScm()
				.getUrl();
			scm.tag = project.getScm()
				.getTag();
			return scm;
		} else {
			return null;
		}
	}

	private String extractUpdateLocation(MavenProject project) {
		if (project.getDistributionManagement() != null) {
			return project.getDistributionManagement()
				.getDownloadUrl();
		} else {
			return null;
		}
	}

	private String extractVendor(MavenProject project) {
		if (project.getOrganization() != null) {
			return project.getOrganization()
				.getName();
		} else {
			return null;
		}
	}

	private VersionDTO extractVersion(MavenProject project) {
		if (project.getVersion() != null && Version.isVersion(project.getVersion())) {
			Version version = Version.parseVersion(project.getVersion());
			VersionDTO versionDto = new VersionDTO();
			versionDto.major = version.getMajor();
			versionDto.minor = version.getMinor();
			versionDto.micro = version.getMicro();
			if (version.getQualifier() != null && !version.getQualifier()
				.isEmpty()) {
				versionDto.qualifier = version.getQualifier();
			}
			return versionDto;
		} else {
			return null;
		}
	}

	private boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
