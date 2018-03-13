package aQute.bnd.deployer.repository.api;

public class Referral {

	private final String	url;
	private final int		depth;

	public Referral(String url, int depth) {
		this.url = url;
		this.depth = depth;
	}

	public String getUrl() {
		return url;
	}

	public int getDepth() {
		return depth;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Referral [");
		if (url != null)
			builder.append("url=")
				.append(url)
				.append(", ");
		builder.append("depth=")
			.append(depth)
			.append(", ");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + depth;
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
		Referral other = (Referral) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		if (depth != other.depth)
			return false;
		return true;
	}

}
