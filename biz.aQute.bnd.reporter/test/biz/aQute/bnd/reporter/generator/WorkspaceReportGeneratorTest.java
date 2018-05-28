package biz.aQute.bnd.reporter.generator;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.plugins.WorkspaceContentsEntryPlugin;
import biz.aQute.bnd.reporter.plugins.XsltTransformerPlugin;
import com.google.common.io.Files;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class WorkspaceReportGeneratorTest extends TestCase {

  private final Set<String> a = new HashSet<>();

  private Workspace getWorkspace() throws Exception {
    final File wsFile = Files.createTempDir();
    wsFile.deleteOnExit();

    final File bnd = new File(wsFile, "build.bnd");
    final File prop = new File(wsFile, "prop.properties");

    bnd.createNewFile();
    bnd.deleteOnExit();

    prop.createNewFile();
    prop.deleteOnExit();
    final Workspace ws = new Workspace(wsFile);

    return ws;
  }

  private Workspace getWorkspacePlus() throws Exception {
    final Workspace ws = getWorkspace();
    final File p = new File(ws.getBase(), "project1");

    final File bnd = new File(p, "bnd.bnd");
    final File t = new File(p, "test.xslt");
    p.mkdir();
    p.deleteOnExit();

    bnd.createNewFile();
    bnd.deleteOnExit();

    t.createNewFile();
    t.deleteOnExit();

    IO.copy(new File("testresources/xslt.xslt"), t);
    return ws;
  }

  public void testWorkspaceReportModel() throws Exception {
    final Workspace ws = getWorkspace();

    try (WorkspaceReportGenerator rg = new WorkspaceReportGenerator(ws)) {
      a.clear();
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "excludes='settings'");
      GeneratorAsserts.verify(rg, 0, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "imports='notFound'");
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "imports='build.bnd:prop:properties'");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "imports='build.bnd::properties'");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "imports='prop.properties: : '");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "imports='prop.properties: '");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "imports='prop.properties'");
      GeneratorAsserts.verify(rg, 2, a, 0);

      a.clear();
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "imports='c ,:c'");
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.addBasicPlugin(new WorkspaceContentsEntryPlugin());
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "includes='settings'");
      GeneratorAsserts.verify(rg, 1, a, 0);

      a.clear();
      ws.addBasicPlugin(new WorkspaceContentsEntryPlugin());
      ws.setProperty(Constants.REPORT_MODEL_WORKSPACE, "@a=cool");
      GeneratorAsserts.verify(rg, 3, a, 0);
    }
  }

  public void testWorkspaceReportGen() throws Exception {
    Workspace ws = getWorkspace();

    try (WorkspaceReportGenerator rg = new WorkspaceReportGenerator(ws)) {

      a.clear();
      a.add(ws.getBase().getCanonicalPath() + File.separator + "test.json");
      ws.setProperty(Constants.REPORT_WORKSPACE, "test.json");
      GeneratorAsserts.verify(rg, 1, a, 1);
    }

    ws = getWorkspacePlus();
    ws.addBasicPlugin(new XsltTransformerPlugin());
    ws.addBasicPlugin(new WorkspaceContentsEntryPlugin());

    try (WorkspaceReportGenerator rg = new WorkspaceReportGenerator(ws)) {

      a.clear();
      a.add(ws.getBase().getCanonicalPath() + File.separator + "project1" + File.separator
          + "test.json");
      ws.setProperty(Constants.REPORT_WORKSPACE, "project1/test.json");
      GeneratorAsserts.verify(rg, 2, a, 1);

      a.clear();
      a.add(ws.getBase().getCanonicalPath() + File.separator + "project1" + File.separator
          + "test2.json");
      ws.getProject("project1").setProperty(Constants.REPORT_PROJECT, "cool.xml");
      ws.setProperty(Constants.REPORT_WORKSPACE,
          "project1/test2.json;template:=project1/test.xslt;param1=test");
      GeneratorAsserts.verify(rg, 2, a, 1);
    }
  }
}
