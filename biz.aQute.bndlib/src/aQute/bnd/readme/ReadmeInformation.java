package aQute.bnd.readme;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Readme content for {@link ReadmeUpdater}.
 */
final public class ReadmeInformation {

	private Optional<String>				_title;
	private Optional<String>				_description;
	private List<ComponentInformation>		_components;
	private List<ContactInformation>		_contacts;
	private Optional<LicenseInformation>	_license;
	private Optional<String>				_copyright;
	private Optional<VendorInformation>		_vendor;
	private Optional<String>				_version;
	private List<ContentInformation>		_contents;

	private ReadmeInformation() {

	}

	public Optional<String> getTitle() {

		return _title;
	}

	public Optional<String> getDescription() {

		return _description;
	}

	public List<ComponentInformation> getComponents() {

		return _components;
	}

	public List<ContactInformation> getContacts() {

		return _contacts;
	}

	public List<ContentInformation> getContents() {

		return _contents;
	}

	public Optional<LicenseInformation> getLicense() {

		return _license;
	}

	public Optional<String> getCopyright() {

		return _copyright;
	}

	public Optional<VendorInformation> getVendor() {

		return _vendor;
	}

	public Optional<String> getVersion() {

		return _version;
	}

	public static Builder builder() {

		return new Builder();
	}

	public static class Builder {

		private String								_title;
		private String								_description;
		private List<ComponentInformationBuilder>	_components	= new LinkedList<>();
		private List<ContactInformation>			_contacts	= new LinkedList<>();
		private List<ContentInformation>			_contents	= new LinkedList<>();
		private LicenseInformation					_license;
		private String								_copyright;
		private VendorInformation					_vendor;
		private String								_version;

		private Builder() {

		}

		public Builder setTitle(String value) {

			_title = value;

			return this;
		}

		public Builder setDescription(String value) {

			_description = value;

			return this;
		}

		public Builder setLicense(String name, String url, String comment) {

			_license = new LicenseInformation(name, url, comment);

			return this;
		}

		public Builder setCopyright(String value) {

			_copyright = value;

			return this;
		}

		public Builder setVendor(String name, String url) {

			_vendor = new VendorInformation(name, url);

			return this;
		}

		public Builder setVersion(String value) {

			_version = value;

			return this;
		}

		public Builder addContent(String name, String relativePath, String description) {

			_contents.add(new ContentInformation(name, relativePath, description));

			return this;
		}

		public ComponentInformationBuilder addComponent() {

			ComponentInformationBuilder b = new ComponentInformationBuilder();

			_components.add(b);

			return b;
		}

		public Builder addContact(String name, String contact, String roles) {

			_contacts.add(new ContactInformation(name, contact, roles));

			return this;
		}

		public ReadmeInformation build() {

			final ReadmeInformation info = new ReadmeInformation();

			info._title = Optional.ofNullable(_title);
			info._description = Optional.ofNullable(_description);
			info._contacts = Collections.unmodifiableList(_contacts);
			info._contents = Collections.unmodifiableList(_contents);
			info._license = Optional.ofNullable(_license);
			info._copyright = Optional.ofNullable(_copyright);
			info._vendor = Optional.ofNullable(_vendor);
			info._version = Optional.ofNullable(_version);

			List<ComponentInformation> components = new LinkedList<>();

			for (ComponentInformationBuilder b : _components) {

				components.add(b.build());
			}

			info._components = Collections.unmodifiableList(components);

			return info;
		}
	}

	public static class ComponentInformation {

		private boolean						_isEnabled;
		private boolean						_isImmediate;
		private String						_policy;
		private String						_scope;
		private String						_factory;
		private String						_name;
		private List<PidInformation>		_pids;
		private List<PropertyInformation>	_properties;
		private List<ServiceInformation>	_services;

		private ComponentInformation(String name, List<ServiceInformation> services,
				List<PropertyInformation> properties, List<PidInformation> pids, String policy, String scope,
				String factory, boolean isImmediate, boolean isEnabled) {
			super();

			_isEnabled = isEnabled;
			_isImmediate = isImmediate;
			_policy = policy != null ? policy : "optional";
			_scope = scope != null ? scope : "singleton";
			_factory = factory != null ? factory : "";
			_name = name != null ? name : "";

			_pids = Collections.unmodifiableList(pids);
			_services = Collections.unmodifiableList(services);
			_properties = new LinkedList<PropertyInformation>();

			LinkedList<PropertyInformation> propertiesNoDescription = new LinkedList<PropertyInformation>();

			for (PropertyInformation p : properties) {

				if (p.getDescription().isEmpty()) {

					propertiesNoDescription.add(p);

				} else {

					_properties.add(p);
				}
			}

			_properties.addAll(propertiesNoDescription);
			_properties = Collections.unmodifiableList(_properties);
		}

		public boolean isEnabled() {
			return _isEnabled;
		}

		public boolean isImmediate() {
			return _isImmediate;
		}

		public String getPolicy() {
			return _policy;
		}

		public String getScope() {
			return _scope;
		}

		public String getFactory() {
			return _factory;
		}

		public List<PidInformation> getPids() {
			return _pids;
		}

		public List<PropertyInformation> getProperties() {
			return _properties;
		}

		public List<ServiceInformation> getServices() {
			return _services;
		}

		public String getName() {
			return _name;
		}
	}

	public static class ComponentInformationBuilder {

		private String						_name;
		private boolean						_isEnabled;
		private boolean						_isImmediate;
		private String						_policy;
		private String						_scope;
		private String						_factory;
		private List<PidInformation>		_pids		= new LinkedList<>();
		private List<PropertyInformation>	_properties	= new LinkedList<>();
		private List<ServiceInformation>	_services	= new LinkedList<>();

		private ComponentInformationBuilder() {

		}

		public ComponentInformationBuilder setName(String value) {

			_name = value;

			return this;
		}

		public ComponentInformationBuilder isImmediate(boolean value) {

			_isImmediate = value;

			return this;
		}

		public ComponentInformationBuilder isEnabled(boolean value) {

			_isEnabled = value;

			return this;
		}

		public ComponentInformationBuilder setConfigurationPolicy(String value) {

			if (value != null) {

				_policy = value.toLowerCase();

			} else {

				_policy = null;
			}

			return this;
		}

		public ComponentInformationBuilder setServiceScope(String value) {

			if (value != null) {

				_scope = value.toLowerCase();

			} else {

				_scope = null;
			}

			return this;
		}

		public ComponentInformationBuilder setFactory(String value) {

			_factory = value;

			return this;
		}

		public ComponentInformationBuilder addPid(String pid, boolean isFactory, List<String> propertyNames) {

			_pids.add(new PidInformation(pid, isFactory, propertyNames));

			return this;
		}

		public ComponentInformationBuilder addProperty(String name, String type, String description,
				Object defaultValue) {

			_properties.add(new PropertyInformation(name, type, description, defaultValue));

			return this;
		}

		public ComponentInformationBuilder addService(String name, String docUrl) {

			_services.add(new ServiceInformation(name, docUrl));

			return this;
		}

		public ComponentInformation build() {

			return new ComponentInformation(_name, _services, _properties, _pids, _policy, _scope, _factory,
					_isImmediate, _isEnabled);
		}
	}

	public static class PidInformation {

		private String			_pid;
		private boolean			_isFactory;
		private List<String>	_propertyNames;

		private PidInformation(String pid, boolean isFactory, List<String> propertyNames) {
			super();

			_pid = pid != null ? pid : "";
			_isFactory = isFactory;
			_propertyNames = propertyNames != null ? Collections.unmodifiableList(new LinkedList<String>(propertyNames))
					: new LinkedList<String>();
		}

		public String getPid() {
			return _pid;
		}

		public List<String> getPropertyNames() {
			return _propertyNames;
		}

		public boolean isFactory() {
			return _isFactory;
		}
	}

	public static class ServiceInformation {

		private String	_name;
		private String	_docUrl;

		private ServiceInformation(String name, String docUrl) {
			super();

			_name = name != null ? name : "";
			_docUrl = docUrl != null ? docUrl : "";
		}

		public String getName() {
			return _name;
		}

		public String getDocUrl() {
			return _docUrl;
		}
	}

	public static class PropertyInformation {

		private String	_name;
		private String	_type;
		private String	_description;
		private String	_defaultValue;

		private PropertyInformation(String name, String type, String description, Object defaultValue) {
			super();

			_name = name != null ? name : "";
			_type = type != null && !type.isEmpty() ? type : "String";
			_description = description != null ? description : "";

			if (defaultValue instanceof Object[]) {

				StringBuffer buff = new StringBuffer();

				if (_type.startsWith("String")) {

					for (Object o : (Object[]) defaultValue) {

						String val = o.toString();

						if (!val.startsWith("\"")) {

							val = "\"" + val;
						}

						if (!val.endsWith("\"") || val.length() == 1) {

							val = val + "\"";
						}

						buff.append(val + ", ");
					}

				} else {

					for (Object o : (Object[]) defaultValue) {

						buff.append(o.toString() + ", ");
					}
				}

				if (((Object[]) defaultValue).length > 0) {

					buff.deleteCharAt(buff.length() - 1);
					buff.deleteCharAt(buff.length() - 1);
				}

				_defaultValue = buff.toString();

			} else {

				_defaultValue = defaultValue != null ? defaultValue.toString() : "";

				if (_type.startsWith("String") && defaultValue != null) {

					if (!_defaultValue.startsWith("\"")) {

						_defaultValue = "\"" + _defaultValue;
					}

					if (!_defaultValue.endsWith("\"") || _defaultValue.length() == 1) {

						_defaultValue = _defaultValue + "\"";
					}
				}
			}
		}

		public String getName() {
			return _name;
		}

		public String getType() {
			return _type;
		}

		public String getDescription() {
			return _description;
		}

		public String getDefaultValue() {
			return _defaultValue;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PropertyInformation) {

				return getName().equals(((PropertyInformation) obj).getName());

			} else {

				return false;
			}
		}

		@Override
		public int hashCode() {

			return getName().hashCode();
		}
	}

	public static class ContentInformation {

		private String	_name;
		private String	_relativePath;
		private String	_description;

		private ContentInformation(String name, String relativePath, String description) {
			super();

			_name = name != null ? name : "";
			_relativePath = relativePath != null ? relativePath : "";
			_description = description != null ? description : "";
		}

		public String getName() {
			return _name;
		}

		public String getRelativePath() {
			return _relativePath;
		}

		public String getDescription() {
			return _description;
		}
	}

	public static class LicenseInformation {

		private String	_name;
		private String	_url;
		private String	_comment;

		private LicenseInformation(String name, String url, String comment) {
			super();

			_name = name != null ? name : "";
			_url = url != null ? url : "";
			_comment = comment != null ? comment : "";
		}

		public String getName() {

			return _name;
		}

		public String getUrl() {

			return _url;
		}

		public String getComment() {

			return _comment;
		}
	}

	public static class ContactInformation {

		private String	_name;
		private String	_contact;
		private String	_roles;

		private ContactInformation(String name, String contact, String roles) {
			super();

			_name = name != null ? name : "";
			_contact = contact != null ? contact : "";
			_roles = roles != null ? roles : "";
		}

		public String getName() {

			return _name;
		}

		public String getContact() {

			return _contact;
		}

		public String getRoles() {

			return _roles;
		}
	}

	public static class VendorInformation {

		private String	_name;
		private String	_url;

		private VendorInformation(String name, String url) {
			super();

			_name = name != null ? name : "";
			_url = url != null ? url : "";
		}

		public String getName() {

			return _name;
		}

		public String getUrl() {

			return _url;
		}
	}
}
