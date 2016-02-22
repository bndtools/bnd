package aQute.bnd.connection.settings;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.util.dto.DTO;

public class SettingsDTO extends DTO {
	public List<ProxyDTO>	proxies	= new ArrayList<>();
	public List<ServerDTO>	servers	= new ArrayList<>();

}
