package aQute.bnd.ant;

import java.io.*;

import aQute.libg.reporter.*;

@Message("bnd messages for Ant")
public interface AntMessages extends Messages {

	ERROR NotAJarFile_(File file);

	ERROR FailedToDeploy_Exception_(File file, Exception e);

	ERROR GotFileNeedDir_(File absoluteFile);

}
