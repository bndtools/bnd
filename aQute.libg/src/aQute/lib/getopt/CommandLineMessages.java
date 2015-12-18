package aQute.lib.getopt;

import java.util.List;

import aQute.service.reporter.Messages;

public interface CommandLineMessages extends Messages {

	ERROR Option__WithArgumentNotLastInAbbreviation_(String name, char charAt, String typeDescriptor);

	ERROR MissingArgument__(String name, char charAt);

	ERROR OptionCanOnlyOccurOnce_(String name);

	ERROR NoSuchCommand_(String cmd);

	ERROR TooManyArguments_(List<String> arguments);

	ERROR MissingArgument_(String string);

	ERROR UnrecognizedOption_(String name);

	ERROR OptionNotSet_(String name);

}
