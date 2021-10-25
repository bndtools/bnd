package aQute.remote.api;

import org.osgi.dto.DTO;

public class XResultDTO extends DTO {

	public static final int	SUCCESS	= 1;
	public static final int	ERROR	= 2;
	public static final int	SKIPPED	= 3;

	public int				result;
	public String			response;

}
