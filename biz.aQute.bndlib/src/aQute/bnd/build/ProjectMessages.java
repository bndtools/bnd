package aQute.bnd.build;

import java.io.*;
import java.util.*;

import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.libg.reporter.*;

public interface ProjectMessages extends Messages {

	ERROR InvalidStrategy(String help, String[] args);

	ERROR RepoTooFewArguments(String help, String[] args);

	ERROR AddingNonExistentFileToClassPath_(File f);

	ERROR Deploying(Exception e);

	ERROR DeployingFile_On_Exception_(File file, String name, Exception e);

	ERROR MissingPom();

	ERROR FoundVersions_ForStrategy_ButNoProvider(SortedMap<Version,RepositoryPlugin> versions, Strategy useStrategy);

	ERROR NoSuchProject(String bsn, String spec);

	ERROR CircularDependencyContext_Message_(String name, String message);

	ERROR IncompatibleHandler_For_(String launcher, String defaultHandler);

	ERROR NoOutputDirectory_(File output);

	ERROR MissingDependson_(String p);

	ERROR NoNameForReleaseRepository();

	ERROR ReleaseRepository_NotFoundIn_(String name, List<RepositoryPlugin> plugins);

	ERROR Release_Into_Exception_(Jar jar, RepositoryPlugin rp, Exception e);

	ERROR NoScripters_(String script);

	ERROR SettingPackageInfoException_(Exception e);

	ERROR ConfusedNoContainerFile();

}
