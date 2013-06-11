package aQute.bnd.mavenplugin;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import aQute.bnd.build.Workspace;

public class Utils {
    public static Workspace getWorkspace(File executionRoot) throws Exception {
        
        File currentFolder = new File("cnf").getCanonicalFile();
        if (currentFolder.exists() && currentFolder.isDirectory())
            return new Workspace(currentFolder.getParentFile());
        
        File sessionRoot = new File(executionRoot,"/cnf").getCanonicalFile();
        if (sessionRoot.exists() && sessionRoot.isDirectory())
            return new Workspace(sessionRoot.getParentFile());
        
        File upFolder = new File("../cnf").getCanonicalFile();
        if (upFolder.exists() && upFolder.isDirectory())
            return new Workspace(upFolder.getParentFile());

        throw new MojoFailureException("No workspace folder found!");
    }

    public static Workspace printIxnfo(String s, Log log, final Workspace workspace) {
        log.info(s);
        if (workspace != null) {
            log.info("Workspace offline     : " + workspace.isOffline());
            log.info("Workspace FailOK      : " + workspace.isFailOk());
            log.info("Workspace Perfect     : " + workspace.isPerfect());
            log.info("Workspace Exceptions  : " + workspace.isExceptions());
            log.info("Workspace Pedantic    : " + workspace.isPedantic());
        }
        return workspace;
    }
}
