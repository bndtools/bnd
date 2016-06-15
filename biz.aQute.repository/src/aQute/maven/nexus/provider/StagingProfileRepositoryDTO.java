package aQute.maven.nexus.provider;

import java.net.URI;

import aQute.bnd.util.dto.DTO;

public class StagingProfileRepositoryDTO extends DTO {
	public String	profileId;
	public String	profileName;

	public String	profileType;
	public String	repositoryId;
	public String	type;
	public String	policy;
	public String	userId;
	public String	userAgent;
	public String	ipAddress;
	public URI		repositoryURI;
	public String	created;
	public String	createdDate;
	public String	createdTimestamp;
	public String	updated;
	public String	updatedDate;
	public String	updatedTimestamp;
	public String	description;
	public String	provider;
	public String	releaseRepositoryId;
	public String	releaseRepositoryName;
	public int		notifications;
	public boolean	transitioning;
}
