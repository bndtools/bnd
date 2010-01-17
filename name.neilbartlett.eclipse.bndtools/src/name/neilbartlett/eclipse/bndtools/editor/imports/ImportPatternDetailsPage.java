package name.neilbartlett.eclipse.bndtools.editor.imports;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

import name.neilbartlett.eclipse.bndtools.UIConstants;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;
import name.neilbartlett.eclipse.bndtools.javamodel.FormPartJavaSearchContext;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.lib.osgi.Constants;

public class ImportPatternDetailsPage extends AbstractFormPart implements
		IDetailsPage, PropertyChangeListener {

	private final ImportPatternsPart listPart;
	
	private BndEditModel model;
	private ImportPattern pattern;
	
	private Text txtPattern;
	private Button btnOptional;
	private Text txtVersionRange;
	
	private AtomicInteger refreshers = new AtomicInteger(0);

	public ImportPatternDetailsPage(ImportPatternsPart importPatternsPart) {
		this.listPart = importPatternsPart;
	}

	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		FieldDecoration assistDecor = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		KeyStroke assistKeyStroke = null;
		try {
			assistKeyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}

		Section mainSection = toolkit.createSection(parent, Section.TITLE_BAR);
		mainSection.setText("Import Pattern Detail");
		
		Composite mainComposite = toolkit.createComposite(mainSection);
		mainSection.setClient(mainComposite);
		
		toolkit.createLabel(mainComposite, "Pattern:");
		txtPattern = toolkit.createText(mainComposite, "");
		ControlDecoration decPattern = new ControlDecoration(txtPattern, SWT.LEFT | SWT.TOP, mainComposite);
		decPattern.setImage(assistDecor.getImage());
		decPattern.setDescriptionText(MessageFormat.format("Content assist is available. Press {0} or start typing to activate", assistKeyStroke.format()));
		decPattern.setShowHover(true);
		decPattern.setShowOnlyOnFocus(true);
		
		ImportPatternProposalProvider proposalProvider = new ImportPatternProposalProvider(new FormPartJavaSearchContext(this));
		ContentProposalAdapter patternProposalAdapter = new ContentProposalAdapter(txtPattern, new TextContentAdapter(), proposalProvider, assistKeyStroke, UIConstants.AUTO_ACTIVATION_CLASSNAME);
		patternProposalAdapter.addContentProposalListener(proposalProvider);
		patternProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
		patternProposalAdapter.setAutoActivationDelay(1000);
		patternProposalAdapter.setLabelProvider(new ImportPatternProposalLabelProvider());
		patternProposalAdapter.addContentProposalListener(new IContentProposalListener() {
			public void proposalAccepted(IContentProposal proposal) {
				ImportPatternProposal patternProposal = (ImportPatternProposal) proposal;
				String toInsert = patternProposal.getContent();
				int currentPos = txtPattern.getCaretPosition();
				txtPattern.setSelection(patternProposal.getReplaceFromPos(), currentPos);
				txtPattern.insert(toInsert);
				txtPattern.setSelection(patternProposal.getCursorPosition());
			}
		});
		
		toolkit.createLabel(mainComposite, ""); // Spacer
		btnOptional = toolkit.createButton(mainComposite, "Optional resolution", SWT.CHECK);
		
		toolkit.createLabel(mainComposite, "Version Range:");
		txtVersionRange = toolkit.createText(mainComposite, "");
		
		/*
		Section attribsSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
		attribsSection.setText("Extra Attributes");
		Composite attribsComposite = toolkit.createComposite(attribsSection);
		*/
		
		// Listeners
		txtPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if(refreshers.get() == 0) {
					pattern.setName(txtPattern.getText());
					listPart.updateLabel(pattern);
					listPart.validate();
					markDirty();
				}
			}
		});
		btnOptional.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(refreshers.get() == 0) {
					pattern.setOptional(btnOptional.getSelection());
					listPart.updateLabel(pattern);
					listPart.validate();
					markDirty();
				}
			}
		});
		txtVersionRange.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if(refreshers.get() == 0) {
					String text = txtVersionRange.getText();
					if(text.length() == 0)
						text = null;
					pattern.setVersionRange(text);
					listPart.updateLabel(pattern);
					listPart.validate();
					markDirty();
				}
			}
		});
		
		// Layout
		GridData gd;
		
		parent.setLayout(new GridLayout(1, false));
		mainSection.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout(2, false));

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalIndent = 5;
		gd.widthHint = 100;
		txtPattern.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.horizontalIndent = 5;
		gd.widthHint = 100;
		txtVersionRange.setLayoutData(gd);
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if(!selection.isEmpty() && selection instanceof IStructuredSelection) {
			this.pattern = (ImportPattern) ((IStructuredSelection) selection).getFirstElement();
		} else {
			this.pattern = null;
		}
		updateFields();
		txtPattern.setFocus();
		txtPattern.setSelection(pattern.getName().length());
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		
		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(Constants.IMPORT_PACKAGE, this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		this.model.removePropertyChangeListener(Constants.IMPORT_PACKAGE, this);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		updateFields();
	}

	void updateFields() {
		try {
			refreshers.incrementAndGet();
			
			txtPattern.setText(pattern != null ? pattern.getName() : "");
			btnOptional.setSelection(pattern != null && pattern.isOptional());
			String versionRange = (pattern != null) ? pattern.getVersionRange() : null;
			txtVersionRange.setText(versionRange != null ? versionRange : "");
		} finally {
			refreshers.decrementAndGet();
		}
	}
	
	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		listPart.commit(onSave);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		Object container = getManagedForm().getContainer();
		
		System.out.println(container);
	}
	
}
