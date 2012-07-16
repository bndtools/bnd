package aQute.bnd.main;

import aQute.bnd.build.*;
import aQute.service.reporter.*;

public interface BndMessages extends Messages {

	ERROR Failed__(Throwable t, String string);

	ERROR UnrecognizedFileType_(String path);

	ERROR MoreArgumentsThanNeeded_(Iterable<String> args);

	ERROR NoCommandForProject(Project p);

	ERROR NoProject();

	ERROR InvalidBumpMask_(String mask);

	ERROR Project_RunFailed_(Project project, Exception e);

	ERROR ForProject_File_FailedToCreateExecutableException_(Project project, String path, Exception e);

	ERROR NoSuchRepository_(String newer);

	ERROR InvalidGlobPattern_(String pattern);

}
