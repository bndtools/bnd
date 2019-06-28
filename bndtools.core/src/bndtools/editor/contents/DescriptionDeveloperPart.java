package bndtools.editor.contents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
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

public class DescriptionDeveloperPart extends SectionPart implements PropertyChangeListener {
	/**
	 * <p>
	 * The properties that can be changed by this part.
	 * </p>
	 * TODO (from issue 443)
	 *
	 * <pre>
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
	 * </pre>
	 */
	private static final String[]	EDITABLE_PROPERTIES	= new String[] {
		Constants.BUNDLE_DOCURL
	};
	private final Set<String>		editablePropertySet;
	private final Set<String>		dirtySet			= new HashSet<>();
	private BndEditModel			model;
	private final Text				bundleDocUrl;
	private final ModificationLock	lock				= new ModificationLock();

	public DescriptionDeveloperPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		Section section = getSection();
		section.setText("Developer Information");
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		// BUNDLE_DOCURL
		toolkit.createLabel(composite, "Documentation URL:");
		bundleDocUrl = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(bundleDocUrl, Constants.BUNDLE_DOCURL);
		bundleDocUrl.addModifyListener(e -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_DOCURL)));
		// Layout
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 10;
		composite.setLayout(layout);
		GridData gd;
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalIndent = 5;
		bundleDocUrl.setLayoutData(gd);
		editablePropertySet = new HashSet<>();
		for (String prop : EDITABLE_PROPERTIES) {
			editablePropertySet.add(prop);
		}
	}

	protected void addDirtyProperty(final String property) {
		lock.ifNotModifying(() -> {
			dirtySet.add(property);
			getManagedForm().dirtyStateChanged();
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
		lock.modifyOperation(() -> {
			String bundleDU = model.getBundleDocUrl();
			bundleDocUrl.setText(bundleDU != null ? bundleDU : ""); //$NON-NLS-1$
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

	@Override
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
