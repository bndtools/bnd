package name.neilbartlett.eclipse.bndtools.wizards.newbundle;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.internal.libs.RefCell;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.SearchUtils;
import name.neilbartlett.eclipse.bndtools.wizards.BundleModel;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleActivator;

public class BundleDetailsWizardPage extends WizardPage {

	private final RefCell<IJavaProject> javaProjectRef;
	private final RefCell<IJavaSearchScope> searchScopeRef;
	private final BundleModel bundleModel;
	
	private Text txtName;
	private Text txtVersion;
	private Label lblActivator;
	private Text txtActivator;
	private Button btnEnableActivator;

	public BundleDetailsWizardPage(String pageName, String title,
			ImageDescriptor titleImage, RefCell<IJavaProject> javaProjectRef, RefCell<IJavaSearchScope> searchScopeRef,
			BundleModel bundleModel) {
		super(pageName, title, titleImage);
		this.javaProjectRef = javaProjectRef;
		this.searchScopeRef = searchScopeRef;
		this.bundleModel = bundleModel;
	}
	

	public void createControl(Composite parent) {
		// Create Controls
		Composite composite = new Composite(parent, SWT.NONE);
		
		Group groupBasic = new Group(composite, SWT.NONE);

		new Label(groupBasic, SWT.NONE).setText("Symbolic Name:");
		txtName = new Text(groupBasic, SWT.BORDER);
		new Label(groupBasic, SWT.NONE).setText("Version:");
		txtVersion = new Text(groupBasic, SWT.BORDER);
		
		
		Group groupActivator = new Group(composite, SWT.NONE);
		btnEnableActivator = new Button(groupActivator, SWT.CHECK);
		btnEnableActivator.setText("Use a bundle activator");

		lblActivator = new Label(groupActivator, SWT.NONE);
		lblActivator.setText("Activator:");

		txtActivator = new Text(groupActivator, SWT.BORDER);
		ControlDecoration decoration = new ControlDecoration(txtActivator,
				SWT.LEFT | SWT.CENTER, composite);
		FieldDecoration contentAssist = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(
						FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		decoration.setImage(contentAssist.getImage());
		decoration.setDescriptionText(contentAssist.getDescription());
		decoration.setShowOnlyOnFocus(true);
		decoration.setShowHover(true);

		ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(txtActivator, new TextContentAdapter(),
				new ActivatorProposalProvider(), null, null);
		proposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		proposalAdapter.setLabelProvider(new ActivatorLabelProvider());
		
		// Add Listeners
		txtName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				bundleModel.setBundleSymbolicName(txtName.getText());
			}
		});
		btnEnableActivator.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean useActivator = btnEnableActivator.getSelection();
				bundleModel.setEnableActivator(useActivator);
				txtActivator.setEnabled(useActivator);
			}
		});
		proposalAdapter.addContentProposalListener(new IContentProposalListener() {
			public void proposalAccepted(IContentProposal proposal) {
				if(proposal instanceof JavaClassContentProposal) {
					bundleModel.setActivatorClass(((JavaClassContentProposal) proposal).getType());
				};
			}
		});
		
		// Layout Controls
		GridDataFactory horizontalFill = GridDataFactory.createFrom(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		composite.setLayout(new GridLayout(1, false));
		groupBasic.setLayoutData(horizontalFill.create());
		groupActivator.setLayoutData(horizontalFill.create());
		
		groupBasic.setLayout(new GridLayout(2, false));
		txtName.setLayoutData(horizontalFill.create());
		txtVersion.setLayoutData(horizontalFill.create());
		
		groupActivator.setLayout(new GridLayout(3, false));
		btnEnableActivator.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true,
				false, 3, 1));
		txtActivator.setLayoutData(horizontalFill.create());
		
		setControl(composite);
	}
	
	@Override
	public boolean isPageComplete() {
		String bundleName = bundleModel.getBundleSymbolicName();
		if(bundleName == null || bundleName.length() == 0) {
			return false;
		}
		
		boolean useActivator = bundleModel.isEnableActivator();
		IType activator = bundleModel.getActivatorClass();
		if(useActivator && activator == null) {
			return false;
		}
		
		return super.isPageComplete();
	}

	@Override
	public void setVisible(boolean visible) {
		if(visible) {
			String bundleName = bundleModel.getBundleSymbolicName();
			boolean useActivator = bundleModel.isEnableActivator();
			IType activator = bundleModel.getActivatorClass();
			
			txtName.setText(bundleName == null ? "" : bundleName);
			if(activator != null) {
				txtActivator.setText(activator.getFullyQualifiedName());
			}
			
			btnEnableActivator.setSelection(useActivator);
			lblActivator.setEnabled(useActivator);
			txtActivator.setEnabled(useActivator);
		}
		
		super.setVisible(visible);
	}

	private class ActivatorProposalProvider implements IContentProposalProvider {

		public IContentProposal[] getProposals(String contents, int position) {
			String substring = contents.substring(0, position).toLowerCase();
			
			final List<IContentProposal> proposals = new LinkedList<IContentProposal>();
			try {
				IJavaProject javaProject = javaProjectRef.getValue();
				if(javaProject == null) {
					proposals.add(new ErrorContentProposal("Not a java project"));
				} else {
					IType activatorType = javaProject.findType(BundleActivator.class.getName());
					
					final SearchRequestor requestor = new SearchRequestor() {
						public void acceptSearchMatch(SearchMatch match) throws CoreException {
							IType element = (IType) match.getElement();
							proposals.add(new JavaClassContentProposal(element));
						}
					};
					
					SearchPattern pattern = SearchPattern.createPattern(activatorType, IJavaSearchConstants.IMPLEMENTORS);
					try {
						NullProgressMonitor monitor = new NullProgressMonitor();
						IJavaSearchScope searchScope = searchScopeRef.getValue();
						if(searchScope != null) {
							new SearchEngine().search(pattern, SearchUtils
									.getDefaultSearchParticipants(), searchScopeRef.getValue(),
									requestor, monitor);
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			} catch (JavaModelException e) {
				proposals.clear();
				proposals.add(new ErrorContentProposal("Error"));
			}
			
			// Remove proposals that don't match what the user has typed
			for (Iterator<IContentProposal> iter = proposals.iterator(); iter.hasNext();) {
				IContentProposal proposal = iter.next();
				if(proposal.getContent().toLowerCase().indexOf(substring) == -1) {
					iter.remove();
				}
			}
			
			return proposals
					.toArray(new IContentProposal[proposals.size()]);
		}
	}
	
	private class JavaClassContentProposal implements IContentProposal {
		
		private final IType element;

		public JavaClassContentProposal(IType element) {
			this.element = element;
		}

		public String getContent() {
			return element.getFullyQualifiedName();
		}

		public int getCursorPosition() {
			return getContent().length();
		}

		public String getDescription() {
			return null;
		}

		public String getLabel() {
			return element.getElementName() + " - " + element.getPackageFragment().getElementName();
		}
		
		public IType getType() {
			return element;
		}
		
	}

	private class ErrorContentProposal implements IContentProposal {

		private final String message;

		public ErrorContentProposal(String message) {
			this.message = message;
		}

		public String getContent() {
			return "";
		}

		public String getDescription() {
			return null;
		}

		public String getLabel() {
			return message;
		}

		public int getCursorPosition() {
			return 0;
		}
	}
	
	private class ActivatorLabelProvider extends LabelProvider {
		
		private Image classImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/class_obj.gif").createImage();
		
		@Override
		public Image getImage(Object element) {
			Image result = null;
			
			if(element instanceof JavaClassContentProposal) {
				result = classImg;
			}
			
			return result;
		}
		
		@Override
		public String getText(Object element) {
			IContentProposal proposal = (IContentProposal) element;
			
			return proposal.getLabel();
		}
		
		@Override
		public void dispose() {
			super.dispose();
			classImg.dispose();
		}
	}

}
