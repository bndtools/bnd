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

public class DescriptionVendorPart extends SectionPart implements PropertyChangeListener {
	/** The properties that can be changed by this part */
	private static final String[]	EDITABLE_PROPERTIES	= new String[] {
		Constants.BUNDLE_VENDOR, Constants.BUNDLE_CONTACTADDRESS
	};
	private final Set<String>		editablePropertySet;
	private final Set<String>		dirtySet			= new HashSet<>();
	private BndEditModel			model;
	private final Text				bundleVendor;
	private final Text				bundleContactAddress;
	private final ModificationLock	lock				= new ModificationLock();

	public DescriptionVendorPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		Section section = getSection();
		section.setText("Vendor Information");
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		// BUNDLE_VENDOR
		toolkit.createLabel(composite, "Vendor:");
		bundleVendor = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(bundleVendor, Constants.BUNDLE_VENDOR);
		bundleVendor.addModifyListener(e -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_VENDOR)));
		// BUNDLE_CONTACTADDRESS
		toolkit.createLabel(composite, "Contact Address:");
		bundleContactAddress = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(bundleContactAddress, Constants.BUNDLE_CONTACTADDRESS);
		bundleContactAddress
			.addModifyListener(e -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_CONTACTADDRESS)));
		// Layout
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 10;
		composite.setLayout(layout);
		GridData gd;
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalIndent = 5;
		bundleVendor.setLayoutData(gd);
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalIndent = 5;
		bundleContactAddress.setLayoutData(gd);
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
			String bundleVndr = model.getBundleVendor();
			bundleVendor.setText(bundleVndr != null ? bundleVndr : ""); //$NON-NLS-1$
			String bundleCA = model.getBundleContactAddress();
			bundleContactAddress.setText(bundleCA != null ? bundleCA : ""); //$NON-NLS-1$
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
