package aQute.bnd.readme;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.readme.ReadmeInformation.ComponentInformationBuilder;

final public class ReadmeProjectOption extends ReadmeOption {

	private ReadmeInformation _information;

	private ReadmeProjectOption() {

	}

	public static Optional<ReadmeProjectOption> parse(Jar source, final String properties, final String rootPath) {
		ReadmeProjectOption option = new ReadmeProjectOption();

		if (option.parse(properties, rootPath, false)) {

			option.extractInformation(source);

			return Optional.of(option);

		} else {

			return Optional.empty();
		}
	}

	@Override
	public String getIncludePath() {

		return super.getIncludePath();
	}

	@Override
	public boolean include() {

		return super.include();
	}

	@Override
	public ReadmeInformation getReadmeInformation() {

		return _information;
	}

	private void extractInformation(Jar source) {

		try {

			Domain domain = Domain.domain(source.getManifest());

			ReadmeInformation.Builder info = ReadmeInformation.builder();

			info.setTitle(domain.getBundleName());
			info.setDescription(domain.getBundleDescription());
			info.setCopyright(domain.getBundleCopyright());
			info.setVersion(domain.getBundleVersion());

			if (domain.getBundleVendor() != null) {
				Matcher m = Pattern.compile("(.*)(https?://.*)", Pattern.CASE_INSENSITIVE)
						.matcher(domain.getBundleVendor());
				String namePart = domain.getBundleVendor();
				String urlPart = null;

				if (m.matches()) {

					info.setVendor(m.group(1), m.group(2));

				} else {

					info.setVendor(domain.getBundleVendor(), null);
				}

			}

			String bundleLicense = source.getManifest().getMainAttributes().getValue(Constants.BUNDLE_LICENSE);

			if (bundleLicense != null) {

				Iterator<Entry<String,Attrs>> licenses = new Parameters(bundleLicense).entrySet().iterator();
				Entry<String,Attrs> license = null;

				while (license == null && licenses.hasNext()) {
					Entry<String,Attrs> l = licenses.next();

					if (l.getKey() != null && !l.getKey().trim().equals("<<EXTERNAL>>")) {

						license = l;
					}
				}

				if (license != null) {

					Map<String,String> attrs = license.getValue();

					info.setLicense(license.getKey().trim(), license.getValue().get("link"),
							license.getValue().get("description"));
				}
			}


			String bundleDevelopers = source.getManifest().getMainAttributes().getValue(Constants.BUNDLE_DEVELOPERS);

			if (bundleDevelopers != null) {

				for (Entry<String,Attrs> developer : new Parameters(bundleDevelopers).entrySet()) {

					String name = developer.getValue().get("name");
					String contact = developer.getValue().get("email");
					String roles = developer.getValue().get("roles");

					if (name == null) {

						name = developer.getKey();
					}

					if (contact == null) {

						contact = developer.getKey();
					}

					info.addContact(name, contact, roles);
				}
			}

			exractComponent(source, domain, info);

			_information = info.build();

		} catch (Exception e) {

			_information = ReadmeInformation.builder().build();
		}
	}

	private static void exractComponent(Jar source, Domain domain, ReadmeInformation.Builder info) {

		ReadmeComponentReader reader = new ReadmeComponentReader(source, domain);

		for (ComponentDescriptionDTO dto : reader.getComponentDescription()) {

			ComponentInformationBuilder componentBuilder = info.addComponent();

			componentBuilder.setName(dto.name);
			componentBuilder.setConfigurationPolicy(dto.configurationPolicy);
			componentBuilder.setServiceScope(dto.scope);
			componentBuilder.setFactory(dto.factory);
			componentBuilder.isEnabled(dto.immediate);
			componentBuilder.isImmediate(dto.defaultEnabled);


			for (String service : dto.serviceInterfaces) {

				componentBuilder.addService(service, null);
			}

			List<String> addedProperties = new LinkedList<>();

			List<String> configPidRevers = Arrays.asList(dto.configurationPid);
			Collections.reverse(configPidRevers);

			for (String pid : configPidRevers) {

				boolean isFactory = false;
				List<String> prop = new LinkedList<>();

				for (ObjectClassDefinitionDTO obdto : reader.getObjectClassDefinitionDTO()) {

					boolean found = false;

					if (obdto.factoryPid.contains(pid)) {

						found = true;
						isFactory = true;
						prop.addAll(obdto.attributes.keySet());

					} else if (obdto.pid.contains(pid)) {

						found = true;
						prop.addAll(obdto.attributes.keySet());
					}

					if (found) {

						for (AttributeDefinitionDTO attdto : obdto.attributes.values()) {

							if (!addedProperties.contains(attdto.id)) {

								addedProperties.add(attdto.id);
								componentBuilder.addProperty(attdto.id, attdto.type, attdto.description,
										attdto.defaultValue);
							}
						}
					}
				}

				componentBuilder.addPid(pid, isFactory, prop);
			}

			for (Entry<String,Object> prop : dto.properties.entrySet()) {

				if (!addedProperties.contains(prop.getKey())) {

					addedProperties.add(prop.getKey());
					componentBuilder.addProperty(prop.getKey(), prop.getValue().getClass().getSimpleName(), null,
							prop.getValue());
				}
			}
		}
	}
}
