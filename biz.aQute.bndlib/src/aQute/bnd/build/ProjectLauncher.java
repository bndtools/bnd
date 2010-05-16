package aQute.bnd.test;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.build.*;
import aQute.lib.osgi.*;
import aQute.libg.header.*;

public class ProjectLauncher extends Processor {
    final Project project;
    static File   runtime;
    File          report;
    Process       process;
    long          timeout = 60 * 60 * 1000;

    public ProjectLauncher(Project project) {
        super(project);
        this.project = project;
    }

    /**
     * Calculate the classpath. We include our own runtime.jar which includes
     * the test framework and we include the first of the test frameworks
     * specified.
     */
    public String[] getClasspath() {
        try {
            List<String> classpath = new ArrayList<String>();
            classpath.add(getRuntime().getAbsolutePath());

            for (Container c : project.getRunpath()) {

                if (c.getType() != Container.TYPE.ERROR) {
                    if (c.getVersion() != null
                            && c.getVersion().equals("project")) {
                        Project sub = c.getProject();
                        sub.clear();
                        File[] outputs = sub.build(false);
                        if (outputs != null)
                            for (File f : outputs)
                                classpath.add(f.getAbsolutePath());

                        getInfo(sub);

                    } else
                    // TODO Yuck, needs to make this more configurable
                    if (!c.getFile().getName().startsWith("ee."))
                        classpath.add(c.getFile().getAbsolutePath());
                } else {
                    error("Invalid entry on the " + Constants.RUNPATH + ": "
                            + c);
                }
            }
            return classpath.toArray(new String[classpath.size()]);
        } catch (Exception e) {
            error("Calculating class path", e);
        }
        return null;
    }

    /**
     * Extract the runtime on the file system so we can refer to it. in the
     * remote VM.
     * 
     * @return
     */
    public static File getRuntime() {
        if (runtime == null) {
            try {
                URL url = ProjectLauncher.class
                        .getResource("/biz.aQute.runtime.jar");
                if (url == null)
                    throw new IllegalStateException(
                            "Can not find my biz.aQute.runtime.jar! Must be in the root");

                runtime = File.createTempFile("biz.aQute.runtime", ".jar");
                runtime.deleteOnExit();
                FileOutputStream out = new FileOutputStream(runtime);
                InputStream in = url.openStream();
                copy(in, out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return runtime;
    }

    public void doRunbundles(List<String> programArguments) throws Exception {
        // we are going to use this for running, so we want to
        // use the "best" version. For compile, we should
        // use the lowest version.
        Collection<Container> testbundles = project.getRunbundles();

        for (Container c : testbundles) {
            if (c.getError() != null)
                error("Invalid bundle on " + Constants.RUNBUNDLES + " "
                        + c.getError());
            else {
                // Do we need to build any sub projects?
                if (c.getVersion() != null && c.getVersion().equals("project")) {
                    if (c.getError() == null && c.getProject() != null) {
                        Project sub = c.getProject();

                        sub.clear();
                        File[] outputs = sub.build(false);
                        for (File f : outputs) {
                            programArguments.add("-bundle");
                            programArguments.add(f.getAbsolutePath());
                        }
                        getInfo(sub);
                    } else {
                        error("Cannot find project "
                                + c.getBundleSymbolicName() + " "
                                + c.getError());
                    }
                } else {
                    programArguments.add("-bundle");
                    programArguments.add(c.getFile().getAbsolutePath());
                }
            }
        }
    }

    private void doRunpath(List<String> programArguments) throws Exception {
        Collection<Container> testpath = project.getRunpath();
        Container found = null;
        for (Container c : testpath) {
            if (c.getAttributes().containsKey("framework")) {
                if (found != null) {
                    warning("Specifying multiple framework classes on the "
                            + Constants.RUNPATH + "\n" + "Previous found: "
                            + found.getProject() + " " + found.getAttributes()
                            + "\n" + "Now found     : " + c.getProject() + " "
                            + c.getAttributes());
                }
                programArguments.add("-framework");
                programArguments.add(c.getAttributes().get("framework"));
                found = c;
            }
            if (c.getAttributes().containsKey("factory")) {
                if (found != null) {
                    warning("Specifying multiple framework factories on the "
                            + Constants.RUNPATH + "\n" + "Previous found: "
                            + found.getProject() + " " + found.getAttributes()
                            + "\n" + "Now found     : " + c.getProject() + " "
                            + c.getAttributes());
                }
                programArguments.add("-framework");
                programArguments.add(c.getAttributes().get("factory"));
                found = c;
            }
            String exports = c.getAttributes().get("export");
            if (exports != null) {
                String parts[] = exports.split("\\s*,\\s*");
                for (String p : parts) {
                    programArguments.add("-export");
                    programArguments.add(p);
                }
            }
        }

        doSystemPackages(programArguments);
        doRunProperties(programArguments);
    }

    private void doRunProperties(List<String> programArguments) {
        Map<String, String> properties = OSGiHeader
                .parseProperties(getProperty(RUNPROPERTIES));
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            programArguments.add("-set");
            programArguments.add(entry.getKey());
            String value = entry.getValue();
            if ( value == null )
                value = "";
            
            if ( value.trim().length() == 0 )
            	value = "\""+value+"\"";
            
            programArguments.add(value);
        }
    }

    private void doSystemPackages(List<String> programArguments) {
        Map<String, Map<String, String>> systemPackages = parseHeader(getProperty(RUNSYSTEMPACKAGES));
        for (Map.Entry<String, Map<String, String>> entry : systemPackages
                .entrySet()) {
            programArguments.add("-export");
            StringBuffer sb = new StringBuffer();
            sb.append(entry.getKey());
            printClause(entry.getValue(), null, sb);
            programArguments.add(sb.toString());
        }
    }

    private void doStorage(List<String> programArguments) throws Exception {
        File tmp = new File(project.getTarget(), "fwtmp");
        tmp.mkdirs();
        tmp.deleteOnExit();

        programArguments.add("-storage");
        programArguments.add(tmp.getAbsolutePath());
    }

    /**
     * Utility to copy a file from a resource.
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    private static void copy(InputStream in, FileOutputStream out)
            throws IOException {
        byte buf[] = new byte[8192];
        int size = in.read(buf);
        while (size > 0) {
            out.write(buf, 0, size);
            size = in.read(buf);
        }
        in.close();
    }

    private Process launch(File[] targets) throws Exception {
        List<String> arguments = newList();
        List<String> vmArguments = newList();

        vmArguments.add(getProperty("java", "java"));
        doClasspath(vmArguments);

        getArguments(targets, vmArguments, arguments);

        vmArguments.add("aQute.junit.runtime.Target");
        arguments.add("-report");
        arguments.add(getTestreport().getAbsolutePath());

        List<String> all = newList();
        all.addAll(vmArguments);
        all.addAll(arguments);

        System.out.println("Cmd: " + all);

        String[] cmdarray = all.toArray(new String[all.size()]);
        if (getErrors().size() > 0)
            return null;

        return Runtime.getRuntime().exec(cmdarray, null, project.getBase());
    }

    /*
     * static Pattern ARGUMENT = Pattern.compile("[-a-zA-Z0-9\\._]+"); private
     * void doVMArguments(List<String> arguments) { Map<String,String> map =
     * OSGiHeader.parseProperties( getProperty(RUNVM)); for ( String key :
     * map.keySet() ) { if ( ARGUMENT.matcher(key).matches()) if (
     * key.startsWith("-")) arguments.add(key); else arguments.add("-D" +
     * key.trim() + "=" + map.get(key)); else warning("VM Argument is not a
     * proper property key: " + key ); } }
     */
    private void doVMArguments(List<String> arguments) {
        Map<String, String> map = OSGiHeader
                .parseProperties(getProperty(RUNVM));
        for (String key : map.keySet()) {
            if (key.startsWith("-"))
                arguments.add(key);
            else
                arguments.add("-D" + key.trim() + "=" + map.get(key));
        }
    }

    private void doClasspath(List<String> arguments) {
        Collection<String> cp = Arrays.asList(getClasspath());
        if (!cp.isEmpty()) {
            arguments.add("-classpath");
            arguments.add(join(cp, File.pathSeparator));
        }
    }

    public int run(File f) throws Exception {
        process = launch(new File[] { f });

        Thread killer = new Thread() {
            public void run() {
                process.destroy();
            }
        };
        Runtime.getRuntime().addShutdownHook(killer);

        Thread killer2 = new Thread("killer2") {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(timeout);
                        System.out
                                .println("EXITING BECAUSE OF OVERALL TIMEOUT: "
                                        + (timeout / 1000) + " secs");
                        process.destroy();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };

        Streamer sin = new Streamer(process.getInputStream(), System.out);
        Streamer serr = new Streamer(process.getErrorStream(), System.out);
        try {
            killer2.start();
            sin.start();
            serr.start();
            return process.waitFor();
        } finally {
            Runtime.getRuntime().removeShutdownHook(killer);
            killer2.interrupt();
            sin.join();
            serr.join();
        }
    }

    static class Streamer extends Thread {
        final InputStream  in;
        final OutputStream out;

        Streamer(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                int c;
                while ((c = in.read()) > 0) {
                    this.out.write(c);
                }
            } catch (IOException ioe) {
                // Ignore
            }
        };

    };

    public File getTestreport() {
        if (report != null)
            return report;
        String path = getProperty(Constants.TESTREPORT,
                "${target}/test-report.xml");
        report = getFile(path);
        return report;

    }

    public void getArguments(List<String> vmArguments,
            List<String> programArguments, boolean undertest) throws Exception {
        File files[] = project.build(undertest);
        getInfo(project);
        if (files == null)
            return;

        getArguments(files, vmArguments, programArguments);
    }

    public void getArguments(File files[], List<String> vmArguments,
            List<String> programArguments) throws Exception {
        doVMArguments(vmArguments);
        doStorage(programArguments);
        doRunpath(programArguments);
        doRunbundles(programArguments);
        for (File file : files) {
            programArguments.add("-target");
            programArguments.add(file.getAbsolutePath());
        }
    }

    public File getReport() {
        return report;
    }

    public File setReport(String report) {
        this.report = getFile(report);
        return this.report;
    }

    public File setReport(File report) {
        this.report = report;
        return this.report;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
