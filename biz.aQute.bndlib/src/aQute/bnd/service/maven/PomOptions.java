package aQute.bnd.service.maven;

import aQute.bnd.util.dto.DTO;

public class PomOptions extends DTO {
	public String	gav;
	public String	parent;
	public boolean	dependencyManagement;
}
