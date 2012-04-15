package aQute.lib.deployer.repository.api;

public abstract class BaseResource {
	
	private final String baseUrl;
	
	protected BaseResource(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}
	
	public abstract String getIdentity();
	
	public abstract String getVersion();

	public abstract String getContentUrl();
	
}
