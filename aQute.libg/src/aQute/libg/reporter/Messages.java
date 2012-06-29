package aQute.libg.reporter;


public interface Messages {
	static public class ERROR {}

	static public class WARNING {}

	ERROR NoSuchFile_(Object r);

	ERROR Unexpected_Error_(String context, Exception e);

}
