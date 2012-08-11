package bndtools.editor.contents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import bndtools.editor.utils.ToolTips;
import bndtools.utils.ModificationLock;

public class DescriptionInfoPart extends SectionPart implements PropertyChangeListener {
    /**
     * <p>
     * The properties that can be changed by this part.
     * </p>
     * TODO (from issue 443)
     * 
     * <pre>
     * Bundle-Name
     *      Should show a default of the bsn (I prefer people to go for the
     *      default, many "nice" names look horrible in a list)
     * Bundle-Category
     *      Must be able to edit as a text line but would be nice if there were
     *      suggestions. Would also be aligned if it was linked to Stackoverflow
     *      categories.
     * Bundle-Icon
     *      Is a header for a number of images. Is always nice to show an
     *      image/icon on the tab so would be great if we could support this.
     *      See bndlib, it has this header. Notice that URLs can be relative to
     *      the JAR.
     * Bundle-Developers
     *      List of email addresses, name attribute should be supported.
     *      This works wonderfully with gravatars.
     *      Example: Peter.Kriens@aQute.biz; name="Peter Kriens", ...
     * Bundle-Contributors
     *      Same as Bundle-Developers
     * Bundle-SCM
     *      A URL as defined in Maven POM to point to the Source Code control
     *      management system
     *      Example: Bundle-SCM: git@github.com:bndtools/bnd.git
     * Bundle-MailingList
     *      Mailing list URI, where to sign up
     *      Example: Bundle-Mailinglist: https://groups.google.com/forum/?fromgroups#!forum/bndtools-users
     * Bundle-Issues
     *      URI to issues tracker
     * Bundle-Revision
     *      A summary for the revision in markdown
     * 
     * All standard OSGi header have a description in aQute.bnd.help.Syntax,
     * which might be useful to get help text with [?] or so.If you do this, I
     * promise to add the missing ones! This also provide patterns for valid
     * values.
     * </pre>
     */
    private static final String[] EDITABLE_PROPERTIES = new String[] {
            Constants.BUNDLE_NAME, /*                                       */
            Constants.BUNDLE_DESCRIPTION, /*                                */
            // Constants.BUNDLE_CATEGORY, /*                                   */
            Constants.BUNDLE_COPYRIGHT, /*                                  */
            // Constants.BUNDLE_LICENSE, /*                                    */
            Constants.BUNDLE_VENDOR, /*                                     */
            Constants.BUNDLE_CONTACTADDRESS, /*                             */
            Constants.BUNDLE_DOCURL
    };
    private final Set<String> editablePropertySet;
    private final Set<String> dirtySet = new HashSet<String>();
    private BndEditModel model;
    private final Text bundleName;
    private final Text bundleDescription;
    //    private final Text bundleCategory;
    private final Text bundleCopyright;
    //    private final Text bundleLicense;
    private final Text bundleVendor;
    private final Text bundleContactAddress;
    private final Text bundleDocUrl;
    private final ModificationLock lock = new ModificationLock();

    public DescriptionInfoPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        Section section = getSection();
        section.setText("Bundle Information");
        Composite composite = toolkit.createComposite(section);
        section.setClient(composite);
        // BUNDLE_NAME
        toolkit.createLabel(composite, "Name:");
        bundleName = toolkit.createText(composite, "", SWT.BORDER);
        ToolTips.setupMessageAndToolTipFromSyntax(bundleName, Constants.BUNDLE_NAME);
        bundleName.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        addDirtyProperty(Constants.BUNDLE_NAME);
                    }
                });
            }
        });
        // BUNDLE_DESCRIPTION
        toolkit.createLabel(composite, "Description:");
        bundleDescription = toolkit.createText(composite, "", SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        ToolTips.setupMessageAndToolTipFromSyntax(bundleDescription, Constants.BUNDLE_DESCRIPTION);
        bundleDescription.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        addDirtyProperty(Constants.BUNDLE_DESCRIPTION);
                    }
                });
            }
        });
        // BUNDLE_CATEGORY
        //        toolkit.createLabel(composite, "Category:");
        //        bundleCategory = toolkit.createText(composite, "", SWT.BORDER);
        //        ToolTips.setupMessageAndToolTipFromSyntax(bundleCategory, Constants.BUNDLE_CATEGORY);
        //        bundleCategory.addModifyListener(new ModifyListener() {
        //            public void modifyText(ModifyEvent e) {
        //                lock.ifNotModifying(new Runnable() {
        //                    public void run() {
        //                        addDirtyProperty(Constants.BUNDLE_CATEGORY);
        //                    }
        //                });
        //            }
        //        });
        // BUNDLE_COPYRIGHT
        toolkit.createLabel(composite, "Copyright:");
        bundleCopyright = toolkit.createText(composite, "", SWT.BORDER);
        ToolTips.setupMessageAndToolTipFromSyntax(bundleCopyright, Constants.BUNDLE_COPYRIGHT);
        bundleCopyright.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        addDirtyProperty(Constants.BUNDLE_COPYRIGHT);
                    }
                });
            }
        });
        // BUNDLE_LICENSE
        //        toolkit.createLabel(composite, "License:");
        //        bundleLicense = toolkit.createText(composite, "", SWT.BORDER);
        //        ToolTips.setupMessageAndToolTipFromSyntax(bundleLicense, Constants.BUNDLE_LICENSE);
        //        bundleLicense.addModifyListener(new ModifyListener() {
        //            public void modifyText(ModifyEvent e) {
        //                lock.ifNotModifying(new Runnable() {
        //                    public void run() {
        //                        addDirtyProperty(Constants.BUNDLE_LICENSE);
        //                    }
        //                });
        //            }
        //        });
        // BUNDLE_VENDOR
        toolkit.createLabel(composite, "Vendor:");
        bundleVendor = toolkit.createText(composite, "", SWT.BORDER);
        ToolTips.setupMessageAndToolTipFromSyntax(bundleVendor, Constants.BUNDLE_VENDOR);
        bundleVendor.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        addDirtyProperty(Constants.BUNDLE_VENDOR);
                    }
                });
            }
        });
        // BUNDLE_CONTACTADDRESS
        toolkit.createLabel(composite, "Contact Address:");
        bundleContactAddress = toolkit.createText(composite, "", SWT.BORDER);
        ToolTips.setupMessageAndToolTipFromSyntax(bundleContactAddress, Constants.BUNDLE_CONTACTADDRESS);
        bundleContactAddress.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        addDirtyProperty(Constants.BUNDLE_CONTACTADDRESS);
                    }
                });
            }
        });
        // BUNDLE_DOCURL
        toolkit.createLabel(composite, "Documentation URL:");
        bundleDocUrl = toolkit.createText(composite, "", SWT.BORDER);
        ToolTips.setupMessageAndToolTipFromSyntax(bundleDocUrl, Constants.BUNDLE_DOCURL);
        bundleDocUrl.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        addDirtyProperty(Constants.BUNDLE_DOCURL);
                    }
                });
            }
        });
        // Layout
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        composite.setLayout(layout);
        GridData gd;
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalIndent = 5;
        bundleName.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalIndent = 5;
        gd.heightHint = 150;
        bundleDescription.setLayoutData(gd);
        //        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        //        gd.horizontalIndent = 5;
        //        bundleCategory.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalIndent = 5;
        bundleCopyright.setLayoutData(gd);
        //        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        //        gd.horizontalIndent = 5;
        //        bundleLicense.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalIndent = 5;
        bundleVendor.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalIndent = 5;
        bundleContactAddress.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalIndent = 5;
        bundleDocUrl.setLayoutData(gd);
        editablePropertySet = new HashSet<String>();
        for (String prop : EDITABLE_PROPERTIES) {
            editablePropertySet.add(prop);
        }
    }

    protected void addDirtyProperty(final String property) {
        lock.ifNotModifying(new Runnable() {
            public void run() {
                dirtySet.add(property);
                getManagedForm().dirtyStateChanged();
            }
        });
    }

    @Override
    public void markDirty() {
        throw new UnsupportedOperationException("Do not call markDirty directly, instead call addDirtyProperty.");
    }

    @Override
    public boolean isDirty() {
        return !dirtySet.isEmpty();
    }

    @Override
    public void commit(boolean onSave) {
        try {
            // Stop listening to property changes during the commit only
            model.removePropertyChangeListener(this);
            if (dirtySet.contains(Constants.BUNDLE_NAME)) {
                String name = bundleName.getText();
                if (name != null && name.length() == 0)
                    name = null;
                model.setBundleName(name);
            }
            if (dirtySet.contains(Constants.BUNDLE_DESCRIPTION)) {
                String name = bundleDescription.getText();
                if (name != null && name.length() == 0)
                    name = null;
                model.setBundleDescription(name);
            }
            //            if (dirtySet.contains(Constants.BUNDLE_CATEGORY)) {
            //                String name = bundleCategory.getText();
            //                if (name != null && name.length() == 0)
            //                    name = null;
            //                model.setBundleCategory(name);
            //            }
            if (dirtySet.contains(Constants.BUNDLE_COPYRIGHT)) {
                String name = bundleCopyright.getText();
                if (name != null && name.length() == 0)
                    name = null;
                model.setBundleCopyright(name);
            }
            //            if (dirtySet.contains(Constants.BUNDLE_LICENSE)) {
            //                String name = bundleLicense.getText();
            //                if (name != null && name.length() == 0)
            //                    name = null;
            //                model.setBundleLicense(name);
            //            }
            if (dirtySet.contains(Constants.BUNDLE_VENDOR)) {
                String name = bundleVendor.getText();
                if (name != null && name.length() == 0)
                    name = null;
                model.setBundleVendor(name);
            }
            if (dirtySet.contains(Constants.BUNDLE_CONTACTADDRESS)) {
                String name = bundleContactAddress.getText();
                if (name != null && name.length() == 0)
                    name = null;
                model.setBundleContactAddress(name);
            }
            if (dirtySet.contains(Constants.BUNDLE_DOCURL)) {
                String name = bundleDocUrl.getText();
                if (name != null && name.length() == 0)
                    name = null;
                model.setBundleDocUrl(name);
            }
        } finally {
            // Restore property change listening
            model.addPropertyChangeListener(this);
            dirtySet.clear();
            getManagedForm().dirtyStateChanged();
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        lock.modifyOperation(new Runnable() {
            public void run() {
                String bundleNm = model.getBundleName();
                bundleName.setText(bundleNm != null ? bundleNm : ""); //$NON-NLS-1$
                String bundleDescr = model.getBundleDescription();
                bundleDescription.setText(bundleDescr != null ? bundleDescr : ""); //$NON-NLS-1$
                //                String bundleCat = model.getBundleCategory();
                //                bundleCategory.setText(bundleCat != null ? bundleCat : ""); //$NON-NLS-1$
                String bundleCR = model.getBundleCopyright();
                bundleCopyright.setText(bundleCR != null ? bundleCR : ""); //$NON-NLS-1$
                //                String bundleLI = model.getBundleLicense();
                //                bundleLicense.setText(bundleLI != null ? bundleLI : ""); //$NON-NLS-1$
                String bundleVndr = model.getBundleVendor();
                bundleVendor.setText(bundleVndr != null ? bundleVndr : ""); //$NON-NLS-1$
                String bundleCA = model.getBundleContactAddress();
                bundleContactAddress.setText(bundleCA != null ? bundleCA : ""); //$NON-NLS-1$
                String bundleDU = model.getBundleDocUrl();
                bundleDocUrl.setText(bundleDU != null ? bundleDU : ""); //$NON-NLS-1$
            }
        });
        dirtySet.clear();
        getManagedForm().dirtyStateChanged();
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);
        this.model = (BndEditModel) form.getInput();
        this.model.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (editablePropertySet.contains(evt.getPropertyName())) {
            IFormPage page = (IFormPage) getManagedForm().getContainer();
            if (page.isActive()) {
                refresh();
            } else {
                markStale();
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.model != null)
            this.model.removePropertyChangeListener(this);
    }

    IJavaProject getJavaProject() {
        IFormPage formPage = (IFormPage) getManagedForm().getContainer();
        IFile file = ResourceUtil.getFile(formPage.getEditorInput());
        return file != null ? JavaCore.create(file.getProject()) : null;
    }
}