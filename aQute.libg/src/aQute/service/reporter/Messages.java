package aQute.service.reporter;

public interface Messages {
	interface ERROR extends Reporter.SetLocation {}

	interface WARNING extends Reporter.SetLocation {}

	ERROR NoSuchFile_(Object r);

	ERROR Unexpected_Error_(String context, Exception e);

}
