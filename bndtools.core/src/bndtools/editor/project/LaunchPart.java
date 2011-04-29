package bndtools.editor.project;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.launch.JUnitShortcut;
import bndtools.launch.RunShortcut;

public class LaunchPart extends SectionPart {

    private Image runImg = null;
    private Image debugImg = null;

    public LaunchPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        Section section = getSection();
        section.setText("Launch");

        Composite composite = new Composite(section, SWT.NONE);
        toolkit.adapt(composite);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        composite.setLayout(new GridLayout(2, true));

        runImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/run.gif").createImage();
        debugImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/debug.gif").createImage();

        ImageHyperlink runOSGiLink = toolkit.createImageHyperlink(composite, SWT.NONE);
        runOSGiLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                launchOSGi("run");
            }
        });
        toolkit.paintBordersFor(runOSGiLink);
        runOSGiLink.setText("Run OSGi");
        runOSGiLink.setImage(runImg);
        runOSGiLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

//        ImageHyperlink runTestLink = toolkit.createImageHyperlink(composite, SWT.NONE);
//        runTestLink.addHyperlinkListener(new HyperlinkAdapter() {
//            @Override
//            public void linkActivated(HyperlinkEvent e) {
//                testOSGi("run");
//            }
//        });
//        toolkit.paintBordersFor(runTestLink);
//        runTestLink.setText("Run Test Framework");
//        runTestLink.setImage(runImg);

        ImageHyperlink debugOSGiLink = toolkit.createImageHyperlink(composite, SWT.NONE);
        toolkit.paintBordersFor(debugOSGiLink);
        debugOSGiLink.setText("Debug OSGi");
        debugOSGiLink.setImage(debugImg);
        debugOSGiLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                launchOSGi("debug");
            }
        });
        debugOSGiLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

//        ImageHyperlink debugTestLink = toolkit.createImageHyperlink(composite, SWT.NONE);
//        toolkit.paintBordersFor(debugTestLink);
//        debugTestLink.setText("Debug Test Framework");
//        debugTestLink.setImage(debugImg);
//        debugTestLink.addHyperlinkListener(new HyperlinkAdapter() {
//            @Override
//            public void linkActivated(HyperlinkEvent e) {
//                testOSGi("debug");
//            }
//        });
    }

    void launchOSGi(String mode) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();

        RunShortcut shortcut = new RunShortcut();
        shortcut.launch(page.getEditor(), mode);
    }

    void testOSGi(String mode) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();

        JUnitShortcut shortcut = new JUnitShortcut();
        shortcut.launch(page.getEditor(), mode);
    }

    @Override
    public void dispose() {
        if (runImg != null) runImg.dispose();
        if (debugImg != null) debugImg.dispose();
        super.dispose();
    }

}
