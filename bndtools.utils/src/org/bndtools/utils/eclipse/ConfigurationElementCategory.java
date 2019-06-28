package org.bndtools.utils.eclipse;

public class ConfigurationElementCategory implements Comparable<ConfigurationElementCategory> {

	private static final String							DEFAULT_SORT	= "zzz";
	public static final ConfigurationElementCategory	DEFAULT			= new ConfigurationElementCategory(DEFAULT_SORT,
		"Others");

	public static ConfigurationElementCategory parse(String data) {
		ConfigurationElementCategory result;

		if (data == null)
			result = DEFAULT;
		else {
			String[] split = data.split("/", 2);
			if (split.length == 1)
				result = new ConfigurationElementCategory(DEFAULT_SORT, split[0].trim());
			else
				result = new ConfigurationElementCategory(split[0].trim(), split[1].trim());
		}

		return result;
	}

	private final String	sort;
	private final String	label;

	public ConfigurationElementCategory(String sort, String label) {
		this.sort = sort;
		this.label = label;
	}

	@Override
	public String toString() {
		return label;
	}

	@Override
	public int compareTo(ConfigurationElementCategory other) {
		return this.sort.compareTo(other.sort);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((sort == null) ? 0 : sort.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfigurationElementCategory other = (ConfigurationElementCategory) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (sort == null) {
			if (other.sort != null)
				return false;
		} else if (!sort.equals(other.sort))
			return false;
		return true;
	}

}
