package aQute.libg.reporter;

import aQute.service.reporter.*;


public interface Messages {
	static public interface ERROR extends Reporter.SetLocation {}

	static public interface WARNING extends Reporter.SetLocation {}

	ERROR NoSuchFile_(Object r);

	ERROR Unexpected_Error_(String context, Exception e);

}
