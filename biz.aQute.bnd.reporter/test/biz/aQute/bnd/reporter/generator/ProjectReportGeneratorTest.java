package biz.aQute.bnd.reporter.generator;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.plugins.ProjectContentsEntryPlugin;
import biz.aQute.bnd.reporter.plugins.XsltTransformerPlugin;
import com.google.common.io.Files;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class ProjectReportGeneratorTest extends TestCase {

  private final Set<String> a = new HashSet<>();

  private Workspace getWorkspace() throws Exception {
    final File wsFile = Files.createTempDir();
    wsFile.deleteOnExit();

    final File bnd = new File(wsFile, "build.bnd");

    bnd.createNewFile();
    bnd.deleteOnExit();

    final Workspace ws = new Workspace(wsFile);

    return ws;
  }

  private Project getProject() throws Exception {
    final Workspace ws = getWorkspace();
    final File p = new File(ws.getBase(), "project1");
    final File bnd = new File(p, "bnd.bnd");
    final File t = new File(p, "test.xslt");
    final File prop = new File(p, "prop.properties");

    p.mkdir();
    p.deleteOnExit();

    bnd.createNewFile();
    bnd.deleteOnExit();

    prop.createNewFile();
    prop.deleteOnExit();

    t.createNewFile();
    t.deleteOnExit();

    IO.copy(new File("testresources/xslt.xslt"), t);

    return ws.getProject("project1");
  }

  private Project getProjectPlus() throws Exception {
    final Project p = getProject();
    final File gene = new File(p.getBase(), "generated");
    final File bundle = new File(gene, "project1.jar");
    final File build = new File(gene, "buildfiles");

    gene.mkdir();
    gene.deleteOnExit();

    bundle.createNewFile();
    bundle.deleteOnExit();

    build.createNewFile();
    build.deleteOnExit();

    IO.copy(new File("testresources/org.component.test.jar"), bundle);

    IO.copy(bundle.getCanonicalPath().getBytes(), build);
    return p;
  }

  public void testProjectReportModel() throws Exception {
    final Project ws = getProject();

    try (ProjectReportGenerator rg = new ProjectReportGenerator(ws)) {
      a.clear();
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "excludes='settings'");
      GeneratorAsserts.verify(rg, 0, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "imports='notFound'");
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "imports='bnd.bnd:prop:properties'");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "imports='bnd.bnd::properties'");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "imports='prop.properties: : '");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "imports='prop.properties: '");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "imports='prop.properties'");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "imports='c ,:c'");
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.addBasicPlugin(new ProjectContentsEntryPlugin());
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "includes='settings'");
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.addBasicPlugin(new ProjectContentsEntryPlugin());
      ws.setProperty(Constants.REPORT_MODEL_PROJECT, "@a=cool");
      GeneratorAsserts.verify(rg, 3, a, 0);
    }
  }

  public void testProjectReportGen() throws Exception {
    Project ws = getProjectPlus();

    try (ProjectReportGenerator rg = new ProjectReportGenerator(ws)) {

      a.clear();
      a.add(ws.getBase().getCanonicalPath() + File.separator + "test.json");
      ws.setProperty(Constants.REPORT_PROJECT, "test.json");
      GeneratorAsserts.verify(rg, 1, a, 1);
    }

    ws = getProjectPlus();
    ws.addBasicPlugin(new XsltTransformerPlugin());
    ws.addBasicPlugin(new ProjectContentsEntryPlugin());

    try (ProjectReportGenerator rg = new ProjectReportGenerator(ws)) {

      a.clear();
      a.add(ws.getBase().getCanonicalPath() + File.separator + "test.json");
      ws.setProperty(Constants.REPORT_PROJECT, "test.json");
      GeneratorAsserts.verify(rg, 2, a, 1);

      a.clear();
      a.add(ws.getBase().getCanonicalPath() + File.separator + "test2.json");
      a.add(ws.getBase().getCanonicalPath() + File.separator + "cool.xml");
      a.add(ws.getBase().getCanonicalPath() + File.separator + "test.json");
      ws.setProperty(Constants.REPORT_BUNDLE, "cool.xml");
      ws.setProperty(Constants.BUNDLE_SYMBOLICNAME, "org.component.test");
      ws.setProperty(Constants.REPORT_PROJECT + ".Merged",
          "test2.json;template:=test.xslt;param1=test");
      GeneratorAsserts.verify(rg, 2, a, 3);
    }
  }
}
