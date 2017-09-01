package aQute.bnd.readme;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.readme.ReadmeInformation.ComponentInformation;
import aQute.bnd.readme.ReadmeInformation.ContactInformation;
import aQute.bnd.readme.ReadmeInformation.ContentInformation;
import aQute.bnd.readme.ReadmeInformation.LicenseInformation;
import aQute.bnd.readme.ReadmeInformation.PidInformation;
import aQute.bnd.readme.ReadmeInformation.PropertyInformation;
import aQute.bnd.readme.ReadmeInformation.ServiceInformation;
import aQute.bnd.readme.ReadmeInformation.VendorInformation;

class ReadmeUtil {

	final static private String	TAG_NAME	= "bnd-gen";
	final static private String	TAG_OPEN	= "<" + TAG_NAME + ">\n\n";
	final static private String	TAG_CLOSE	= "</" + TAG_NAME + ">\n\n";

	public static int findPosition(StringBuffer readme, int index) {

		notNull(readme, "readme");

		if (index <= 0) {

			return 0;

		} else {

			Pattern pattern = Pattern.compile("^[ \t]*#[ \t]+.*$", Pattern.MULTILINE);

			Matcher matcher = pattern.matcher(readme);

			int position = 0;
			int maxIndex = index + 1;
			boolean condition = true;

			for (int i = 0; i < maxIndex && condition; i++) {

				if (matcher.find()) {

					position = matcher.start();

				} else {

					position = readme.length();
					condition = false;
				}
			}

			return position;
		}
	}

	public static StringBuffer removeAllTags(String readme) {

		notNull(readme, "readme");

		Pattern pattern = Pattern.compile("<[ \t]*" + TAG_NAME + ".*?<[ \t]*\\/[ \t]*" + TAG_NAME + ".*?>(\n)?(\n)?",
				Pattern.DOTALL);

		return new StringBuffer(pattern.matcher(readme).replaceAll(""));
	}

	public static String generateTitle(String title) {

		notNull(title, "title");

		return TAG_OPEN + "# " + title + TAG_CLOSE;
	}

	public static String generateDescription(String description) {

		notNull(description, "description");

		return TAG_OPEN + description + TAG_CLOSE;
	}

	public static String generateTitleDescription(String title, String description) {

		notNull(title, "title");
		notNull(description, "description");

		return TAG_OPEN + "# " + title + "\n\n" + description + TAG_CLOSE;
	}

	public static String generateComponents(Collection<ComponentInformation> components, String message) {

		notNull(components, "components");
		notNull(message, "message");

		StringBuilder sb = new StringBuilder();

		sb.append(TAG_OPEN + "# Components");

		for (ComponentInformation component : components) {

			if (component.isEnabled()) {

				sb.append("\n\n## " + component.getName() + " - `enabled`\n\n");

			} else {

				sb.append("\n\n## " + component.getName() + " - `not enabled`\n\n");
			}

			sb.append("### Services\n\n");

			if (component.getServices().isEmpty()) {

				sb.append("This component does not provide services.\n\n");

			} else {

				sb.append("This component provides the following services:\n\n");
				sb.append("|Service	|\n");
				sb.append("|---	|\n");

				for (ServiceInformation service : component.getServices()) {

					if (service.getDocUrl().isEmpty()) {

						sb.append("|" + service.getName() + "	|\n");

					} else {
						sb.append("|[" + service.getName() + "](" + service.getDocUrl() + ")	|\n");

					}
				}

				sb.append("\n");
			}

			sb.append("### Properties\n\n");

			if (component.getProperties().isEmpty()) {

				sb.append("This component does not define properties.\n\n");

			} else {

				sb.append("This component has the following properties:\n\n");

				sb.append("|Name	|Type	|Description	|Default	|\n");
				sb.append("|---	|---	|---	|---	|\n");

				for (PropertyInformation properties : component.getProperties()) {

					sb.append("|" + properties.getName() + "	|");
					sb.append(properties.getType() + "	|");
					sb.append(properties.getDescription() + "	|");
					sb.append(properties.getDefaultValue() + "	|\n");
				}

				sb.append("\n");
			}

			sb.append("### Configuration\n\n");

			if (component.getPolicy().equals("ignore")) {

				sb.append("This component do not use any configuration.\n\n");

			} else {

				if (component.getPolicy().equals("optional")) {

					sb.append("This component can `optionally` be configured with the following Pids:\n\n");

				} else {

					sb.append("This component `must be` configured with the following Pids:\n\n");
				}

				for (PidInformation pid : component.getPids()) {

					if (pid.isFactory()) {

						sb.append("* **Pid:	" + pid.getPid() + "**	-	`factory`\n");

					} else {

						sb.append("* **Pid:	" + pid.getPid() + "**\n");
					}

					if (pid.getPropertyNames().isEmpty()) {

						sb.append("\n");

					} else {

						sb.append("\n	Properties:	");

						for (String p : pid.getPropertyNames()) {

							sb.append(p + ", ");
						}

						sb.deleteCharAt(sb.length() - 1);
						sb.deleteCharAt(sb.length() - 1);
						sb.append("\n\n");
					}
				}

				sb.append("\n");
			}

			sb.append("### Lifecycle\n\n");

			if (!component.getFactory().isEmpty()) {

				sb.append("This component is a `factory component`, it must be managed with a **FactoryComponent** ");
				sb.append("service with the following property:\n```\ncomponent.factory=");
				sb.append(component.getFactory());
				sb.append("\n```\n");

			} else if (component.getServices().isEmpty() || component.isImmediate()) {

				sb.append(
						"This component is an `immediate component`, it will be activated on bundle start as soon as all its requirements will be satisfied.\n");

			} else {

				String scope;

				switch (component.getScope()) {
					case "bundle" :
						scope = "bundle";
						break;
					case "prototype" :
						scope = "prototype";
						break;
					case "singleton" :
						scope = "singleton";
						break;
					default :
						scope = "singleton";
						break;
				}

				sb.append("This component is a `delayed component` with a `" + scope
						+ "` scope, it will be activated if needed as soon as all its requirements will be satisfied.\n");

			}

			sb.append("\n");
		}

		if (!message.isEmpty()) {

			sb.append("## Additional Information\n\n" + message + "\n");
		}

		sb.append(TAG_CLOSE);

		return sb.toString();
	}

	public static String generateContacts(Collection<ContactInformation> contacts, String message) {

		notNull(contacts, "contacts");
		notNull(message, "message");

		StringBuilder sb = new StringBuilder();

		sb.append(TAG_OPEN);
		sb.append("# Contacts");

		if (!message.isEmpty()) {

			sb.append("\n\n");
			sb.append(message);
		}

		if (!contacts.isEmpty()) {

			sb.append("\n\n");

			sb.append("|Name	|Contact	|Roles	|\n");
			sb.append("|---	|---	|---	|\n");

			for (ContactInformation contact : contacts) {

				if (contact.getContact().contains("@")) {

					sb.append("|" + contact.getName() + "	|[" + contact.getContact() + "](mailto:"
							+ contact.getContact() + ")	|" + contact.getRoles() + "	|\n");

				} else {

					sb.append("|" + contact.getName() + "	|" + contact.getContact() + "	|" + contact.getRoles()
							+ "	|\n");
				}
			}
		}

		sb.append(TAG_CLOSE);

		return sb.toString();
	}

	public static String generateLicense(LicenseInformation license) {

		notNull(license, "license");

		StringBuilder sb = new StringBuilder();

		sb.append(TAG_OPEN);
		sb.append("# License");

		if (!license.getName().isEmpty()) {

			sb.append("\n\n");
			sb.append("[" + license.getName() + "](" + license.getUrl() + ")");

		} else {

			sb.append("\n\n");
			sb.append("[" + license.getUrl() + "](" + license.getUrl() + ")");
		}

		if (!license.getComment().isEmpty()) {

			sb.append("\n\n");
			sb.append(license.getComment());
		}

		sb.append(TAG_CLOSE);

		return sb.toString();
	}

	public static String generateCopyright(String copyright) {

		notNull(copyright, "copyright");

		return TAG_OPEN + "# Copyright\n\n" + copyright + TAG_CLOSE;
	}

	public static String generateContents(Collection<ContentInformation> contents) {

		notNull(contents, "contents");

		StringBuilder sb = new StringBuilder();

		sb.append(TAG_OPEN);
		sb.append("# Contents\n\n");

		for (ContentInformation content : contents) {

			String name = content.getName();

			if (name.isEmpty()) {

				name = content.getRelativePath();
			}

			if (name.isEmpty()) {

				name = "-";
			}

			if (content.getRelativePath().isEmpty()) {

				sb.append("* **" + name + "**");

			} else {

				sb.append("* [**" + name + "**](" + content.getRelativePath() + ")");
			}

			if (content.getDescription().isEmpty()) {

				sb.append("\n");

			} else {

				sb.append(": " + content.getDescription() + "\n");
			}
		}

		sb.append(TAG_CLOSE);

		return sb.toString();
	}

	public static String generateVendorVersion(VendorInformation vendor, String version) {

		notNull(version, "version");
		notNull(vendor, "vendor");

		StringBuilder sb = new StringBuilder();

		sb.append(TAG_OPEN + "---\n");

		if (vendor.getUrl().isEmpty()) {

			sb.append(vendor.getName() + " - version " + version);

		} else {

			sb.append("[" + vendor.getName() + "](" + vendor.getUrl() + ") - version " + version);
		}

		sb.append(TAG_CLOSE);

		return sb.toString();
	}


	public static String generateVersion(String version) {

		notNull(version, "version");

		return TAG_OPEN + "---\nversion " + version + TAG_CLOSE;
	}

	public static String generateVendor(VendorInformation vendor) {

		notNull(vendor, "vendor");

		StringBuilder sb = new StringBuilder();

		sb.append(TAG_OPEN + "---\n");

		if (vendor.getUrl().isEmpty()) {

			sb.append(vendor.getName());

		} else {

			sb.append("[" + vendor.getName() + "](" + vendor.getUrl() + ")");
		}

		sb.append(TAG_CLOSE);

		return sb.toString();
	}

	static void notNull(Object arg, String name) {

		if (arg == null) {

			throw new NullPointerException(name);
		}
	}
}
