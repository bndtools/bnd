package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import bndtools.utils.ModificationLock;

public class SubBundlesPart extends SectionPart implements PropertyChangeListener {

	private static final String		ALL_BND					= "*.bnd";
	private static final String		WARNING_EDITED_MANUALLY	= "WARNING_EDITED_MANUALLY";

	private final ModificationLock	lock					= new ModificationLock();

	private BndEditModel			model;
	private List<String>			subBundleList;
	private Button					button;

	public SubBundlesPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	final void createSection(Section section, FormToolkit toolkit) {
		section.setText("Sub-bundles");
		section.setDescription(
			"If sub-bundles are enabled, then .bnd files other than \"bnd.bnd\" will be built as bundles.");

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		button = new Button(composite, SWT.CHECK);
		button.setText("Enable sub-bundles");

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lock.ifNotModifying(() -> {
					if (button.getSelection()) {
						subBundleList = Arrays.asList(new String[] {
							ALL_BND
						});
					} else {
						subBundleList = null;
					}
					markDirty();
					model.setSubBndFiles(subBundleList);
				});
			}
		});

		// table = toolkit.createTable(composite, SWT.FULL_SELECTION |
		// SWT.MULTI);
		// table.setHeaderVisible(false);
		// table.setLinesVisible(false);
		//
		// viewer = new TableViewer(table);
		// viewer.setContentProvider(new ArrayContentProvider());
		// viewer.setLabelProvider(n());

		// Listeners
		// viewer.addSelectionChangedListener(new ISelectionChangedListener() {
		// public void selectionChanged(SelectionChangedEvent event) {
		// removeItem.setEnabled(!viewer.getSelection().isEmpty());
		// }
		// });
		// addItem.addSelectionListener(new SelectionAdapter() {
		// @Override
		// public void widgetSelected(SelectionEvent e) {
		// doAdd();
		// }
		// });
		// removeItem.addSelectionListener(new SelectionAdapter() {
		// @Override
		// public void widgetSelected(SelectionEvent e) {
		// doRemove();
		// }
		// });

		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 75;
		// table.setLayoutData(gd);
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(Constants.SUB, this);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (model != null)
			model.removePropertyChangeListener(Constants.SUB, this);
	}

	@Override
	public void refresh() {
		subBundleList = model.getSubBndFiles();

		lock.modifyOperation(() -> {
			IMessageManager msgs = getManagedForm().getMessageManager();
			Control control = getSection().getDescriptionControl();
			if (control == null)
				control = getSection().getClient();

			if (subBundleList == null || subBundleList.isEmpty()) {
				button.setGrayed(false);
				button.setSelection(false);
				msgs.removeMessage(WARNING_EDITED_MANUALLY, control);
			} else if (subBundleList.size() == 1 && subBundleList.iterator()
				.next()
				.equalsIgnoreCase(ALL_BND)) {
				button.setGrayed(false);
				button.setSelection(true);
				msgs.removeMessage(WARNING_EDITED_MANUALLY, control);
			} else {
				button.setGrayed(true);
				button.setSelection(true);
				msgs.addMessage(WARNING_EDITED_MANUALLY,
					"The '-sub' setting has been edited manually in the bnd.bnd file. Changing here will override the manually provided setting.",
					null, IMessageProvider.WARNING, control);
			}
		});
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// model changes already committed
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		if (page.isActive()) {
			refresh();
		} else {
			markStale();
		}
	}
}
