package bndtools.editor.pages;

import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import aQute.bnd.build.model.BndEditModel;
import bndtools.editor.common.MDSashForm;
import bndtools.editor.contents.DescriptionBundlePart;
import bndtools.editor.contents.DescriptionDeveloperPart;
import bndtools.editor.contents.DescriptionRightsPart;
import bndtools.editor.contents.DescriptionVendorPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class BundleDescriptionPage extends FormPage {
	private final BndEditModel				model;
	private Color							greyTitleBarColour;

	public static final IFormPageFactory	FACTORY	= new IFormPageFactory() {
														@Override
														public IFormPage createPage(ExtendedFormEditor editor,
															BndEditModel model, String id)
															throws IllegalArgumentException {
															return new BundleDescriptionPage(editor, model, id,
																"Description");
														}

														@Override
														public boolean supportsMode(Mode mode) {
															return mode == Mode.bundle;
														}
													};

	public BundleDescriptionPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		managedForm.setInput(model);

		ScrolledForm scrolledForm = managedForm.getForm();
		scrolledForm.setText("Bundle Description");

		Form form = scrolledForm.getForm();
		toolkit.decorateFormHeading(form);
		form.addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));
		Composite body = form.getBody();

		greyTitleBarColour = new Color(body.getDisplay(), 210, 245, 210);

		// Create controls
		MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
		sashForm.setSashWidth(2);
		toolkit.adapt(sashForm, false, false);

		Composite leftPanel = toolkit.createComposite(sashForm);

		DescriptionBundlePart infoPart = new DescriptionBundlePart(leftPanel, toolkit,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		managedForm.addPart(infoPart);

		DescriptionRightsPart rightsPart = new DescriptionRightsPart(leftPanel, toolkit,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		managedForm.addPart(rightsPart);

		DescriptionVendorPart vendorPart = new DescriptionVendorPart(leftPanel, toolkit,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		managedForm.addPart(vendorPart);

		DescriptionDeveloperPart developerPart = new DescriptionDeveloperPart(leftPanel, toolkit,
			ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		managedForm.addPart(developerPart);

		// LAYOUT
		GridData gd;
		GridLayout layout;

		layout = new GridLayout(1, false);
		leftPanel.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		infoPart.getSection()
			.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		rightsPart.getSection()
			.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		vendorPart.getSection()
			.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		developerPart.getSection()
			.setLayoutData(gd);

		sashForm.hookResizeListener();

		// Layout
		body.setLayout(new FillLayout());
	}

	@Override
	public void dispose() {
		super.dispose();
		if (greyTitleBarColour != null)
			greyTitleBarColour.dispose();
	}
}
