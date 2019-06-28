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

public class DescriptionBundlePart extends SectionPart implements PropertyChangeListener {
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
	 *
	 * All standard OSGi header have a description in aQute.bnd.help.Syntax,
	 * which might be useful to get help text with [?] or so.If you do this, I
	 * promise to add the missing ones! This also provide patterns for valid
	 * values.
	 * </pre>
	 */
	private static final String[]	EDITABLE_PROPERTIES	= new String[] {
		Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_CATEGORY
	};
	private final Set<String>		editablePropertySet;
	private final Set<String>		dirtySet			= new HashSet<>();
	private BndEditModel			model;
	private final Text				bundleName;
	private final Text				bundleDescription;
	private final Text				bundleCategory;
	private final ModificationLock	lock				= new ModificationLock();

	public DescriptionBundlePart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		Section section = getSection();
		section.setText("Bundle Information");
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		// BUNDLE_NAME
		toolkit.createLabel(composite, "Name:");
		bundleName = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(bundleName, Constants.BUNDLE_NAME);
		bundleName.addModifyListener(e -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_NAME)));
		// BUNDLE_DESCRIPTION
		toolkit.createLabel(composite, "Description:");
		bundleDescription = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(bundleDescription, Constants.BUNDLE_DESCRIPTION);
		bundleDescription
			.addModifyListener(e -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_DESCRIPTION)));
		// BUNDLE_CATEGORY
		toolkit.createLabel(composite, "Category:");
		bundleCategory = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(bundleCategory, Constants.BUNDLE_CATEGORY);
		bundleCategory.addModifyListener(e -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_CATEGORY)));
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
		bundleDescription.setLayoutData(gd);
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalIndent = 5;
		bundleCategory.setLayoutData(gd);
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
			if (dirtySet.contains(Constants.BUNDLE_CATEGORY)) {
				String name = bundleCategory.getText();
				if (name != null && name.length() == 0)
					name = null;
				model.setBundleCategory(name);
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
			String bundleNm = model.getBundleName();
			bundleName.setText(bundleNm != null ? bundleNm : ""); //$NON-NLS-1$
			String bundleDescr = model.getBundleDescription();
			bundleDescription.setText(bundleDescr != null ? bundleDescr : ""); //$NON-NLS-1$
			String bundleCat = model.getBundleCategory();
			bundleCategory.setText(bundleCat != null ? bundleCat : ""); //$NON-NLS-1$
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
