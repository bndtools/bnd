package aQute.bnd.readme;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;

abstract class ReadmeOption {

	private ReadmeConfiguration	_configuration	= null;
	private String				_readmePath		= null;
	private boolean				_include		= false;
	private String				_includePath	= null;

	final public String getReadmeFilePath() {

		return _readmePath;
	}

	final public boolean isReadmeFile() {

		return _readmePath.matches("^.*\\.(md|markdown|mdown|mkdn|mkd)$");
	}

	final public ReadmeConfiguration getReadmeConfiguration() {

		return _configuration;
	}

	protected String getIncludePath() {

		return _includePath;
	}

	protected boolean include() {

		return _include;
	}

	public abstract ReadmeInformation getReadmeInformation();

	final protected boolean parse(String properties, String rootPath, boolean isWorkspace) {

		if (rootPath == null) {

			throw new NullPointerException("rootPath is null, a root path must be provided");
		}

		Attrs attributes = getAttributes(properties, isWorkspace);

		if (attributes != null) {

			extractConfiguration(attributes, isWorkspace);
			extractPath(attributes, rootPath, isWorkspace);
			_include = attributes.containsKey("include") ? Boolean.parseBoolean(attributes.get("include")) : true;

			return true;

		} else {

			return false;
		}
	}

	private Attrs getAttributes(String properties, boolean isWorkspace) {

		if (properties == null || properties.isEmpty()) {

			return null;

		} else {

			Attrs attributes = OSGiHeader.parseProperties(properties);

			if (attributes.containsKey("false")) {

				return null;

			} else if (attributes.containsKey("workspace") && !isWorkspace) {

				return null;

			} else if (attributes.containsKey("project") && isWorkspace) {

				return null;
			}

			return attributes;
		}
	}

	private void extractConfiguration(final Attrs attributes, final boolean isWorkspace) {

		ReadmeConfiguration.Builder builder = ReadmeConfiguration.builder();

		boolean defaultValue = true;

		defaultValue = getShow(ReadmeProperties.SHOW_ALL, attributes, isWorkspace, true);

		builder.showDefault(defaultValue);
		builder.showTitle(getShow(ReadmeProperties.SHOW_TITLE, attributes, isWorkspace, defaultValue));
		builder.showDescription(getShow(ReadmeProperties.SHOW_DESCRIPTION, attributes, isWorkspace, defaultValue));
		builder.showComponents(getShow(ReadmeProperties.SHOW_COMPONENTS, attributes, isWorkspace, defaultValue));
		builder.showLicense(getShow(ReadmeProperties.SHOW_LICENSE, attributes, isWorkspace, defaultValue));
		builder.showCopyright(getShow(ReadmeProperties.SHOW_COPYRIGHT, attributes, isWorkspace, defaultValue));
		builder.showContacts(getShow(ReadmeProperties.SHOW_CONTACTS, attributes, isWorkspace, defaultValue));
		builder.showContents(getShow(ReadmeProperties.SHOW_CONTENTS, attributes, isWorkspace, defaultValue));
		builder.showVendor(getShow(ReadmeProperties.SHOW_VENDOR, attributes, isWorkspace, defaultValue));
		builder.showVersion(getShow(ReadmeProperties.SHOW_VERSION, attributes, isWorkspace, defaultValue));
		builder.contactsMessage(attributes.get(ReadmeProperties.CONTACTS_MESSAGE));
		builder.componentsMessage(attributes.get(ReadmeProperties.COMPONENTS_MESSAGE));

		if (attributes.get(ReadmeProperties.COMPONENTS_INDEX) != null) {

			try {

				builder.componentsIndex(Integer.parseInt(attributes.get(ReadmeProperties.COMPONENTS_INDEX)));

			} catch (NumberFormatException e) {
				// Nothing to do
			}
		}

		_configuration = builder.build();
	}

	private boolean getShow(String att, Attrs attributes, boolean isWorkspace, boolean defaultValue) {

		String value = attributes.get(att);

		if (attributes.containsKey(att)) {
			if (Boolean.parseBoolean(value)) {

				return true;

			} else if (isWorkspace && value.equals("workspace")) {

				return true;

			} else if (!isWorkspace && value.equals("project")) {

				return true;

			} else {

				return false;
			}

		} else {

			return defaultValue;
		}
	}

	private void extractPath(final Attrs attributes, final String rootPath, final boolean isWorkspace) {

		if (isWorkspace) {

			_includePath = attributes.get(ReadmeProperties.WORKSPACE_PATH) != null
					? attributes.get(ReadmeProperties.WORKSPACE_PATH)
					: rootPath + "/readme.md";

			_readmePath = _includePath;

		} else {

			_includePath = attributes.get(ReadmeProperties.PATH) != null ? attributes.get(ReadmeProperties.PATH)
					: rootPath + "/readme.md";

			_readmePath = _includePath;

			if (_readmePath.startsWith("{") && _readmePath.endsWith("}")) {

				_readmePath = _readmePath.substring(1, _readmePath.length() - 1).trim();
			}

			String parts[] = _readmePath.split("\\s*=\\s*");
			_readmePath = parts[0];

			if (parts.length == 2) {

				_readmePath = parts[1];
			}
		}
	}
}
