package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.libg.reporter.*;

public class BaseTask extends Task implements Reporter {
    List<String>    errors   = new ArrayList<String>();
    List<String>    warnings = new ArrayList<String>();
    List<String>    progress = new ArrayList<String>();
    boolean pedantic;
    boolean trace;
    String onfail;
    
    public void error(String s, Object... args ) {
        errors.add(String.format(s, args));
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getProgress() {
        return progress;
    }

    public List<String> getWarnings() {
        // TODO Auto-generated method stub
        return warnings;
    }

    public void progress(String s, Object ... args) {
        progress.add(String.format(s,args));
    }

    public void warning(String s, Object ... args) {
        warnings.add(String.format(s, args));
    }

    protected boolean report() {
        return report(this);
    }

    protected boolean report(Reporter reporter) {
        if (reporter.getWarnings().size() > 0) {
            System.err.println("Warnings");
            for (Iterator<String> e = reporter.getWarnings().iterator(); e.hasNext();) {
                System.err.println(" " + e.next());
            }
        }
        if (reporter.getErrors().size() > 0) {
            System.err.println( reporter.getErrors().size() + " Errors");
            for (Iterator<String> e = reporter.getErrors().iterator(); e.hasNext();) {
                System.err.println(" " + e.next());
            }
            return true;
        }
        return false;
    }

    public static File getFile(File base, String file)  {
        File f = new File(file);
        if (!f.isAbsolute()) {
            int n;

            f = base.getAbsoluteFile();
            while ((n = file.indexOf('/')) > 0) {
                String first = file.substring(0, n);
                file = file.substring(n + 1);
                if (first.equals(".."))
                    f = f.getParentFile();
                else
                    f = new File(f, first);
            }
            f = new File(f, file);
        }
        try {
            return f.getCanonicalFile();
        } catch(IOException e ) {
            return f.getAbsoluteFile();
        }
    }

    protected List<String> split(String dependsOn, String string) {
        if (dependsOn == null)
            return new ArrayList<String>();

        return Arrays.asList(string.split("\\s*" + string + "\\s*"));
    }

    protected String join(Collection<?> classpath, String string) {
        StringBuilder sb = new StringBuilder();
        String del = "";
        for (Object name : classpath) {
            sb.append(del);
            sb.append(name);
            del = string;
        }
        return sb.toString();
    }

    public boolean isPedantic() {
        return pedantic;
    }

    public void setPedantic(boolean pedantic) {
        this.pedantic = pedantic;
    }
    public void setTrace(boolean trace) {
        this.trace = trace;
    }
    public boolean isTrace() {
    	return trace;
    }
    public void trace(String s, Object... args) {
        System.out.printf("# "+s+"\n", args);
    }

}
