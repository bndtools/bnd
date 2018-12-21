package aQute.lib.dtoformatter;

public interface ObjectFormatter {
	int	INSPECT	= 0;
	int	LINE	= 1;
	int	PART	= 2;

	CharSequence format(Object o, int level, ObjectFormatter formatter);
}
