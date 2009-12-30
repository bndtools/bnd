/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class GeneralInfoPart extends SectionPart implements PropertyChangeListener {
	
	private static final String AUTO_ACTIVATE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890._";

	private BndEditModel model;
	
	private Text txtBSN;
	private Text txtVersion;
	
	private AtomicBoolean inRefresh = new AtomicBoolean(false);

	private Text txtActivator;

	public GeneralInfoPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		createSection(getSection(), toolkit);
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("General Information");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, "Symbolic Name:");
		txtBSN = toolkit.createText(composite, "");
		
		toolkit.createLabel(composite, "Version:");
		txtVersion = toolkit.createText(composite, "");
		
		toolkit.createLabel(composite, "Activator:");
		txtActivator = toolkit.createText(composite, "");
		
		KeyStroke keyStroke = null;
		try {
			keyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}
		ControlDecoration decorActivator = new ControlDecoration(txtActivator, SWT.LEFT | SWT.TOP, composite);
		FieldDecoration contentAssist = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		decorActivator.setImage(contentAssist.getImage());
		decorActivator.setDescriptionText(MessageFormat.format("Content Assist is available. Press {0} or start typing to activate", keyStroke.format()));
		decorActivator.setShowOnlyOnFocus(true);
		decorActivator.setShowHover(true);

		ContentProposalAdapter activatorProposalAdapter = new ContentProposalAdapter(txtActivator, new TextContentAdapter(), new ActivatorProposalProvider(), keyStroke, AUTO_ACTIVATE_CHARS.toCharArray());
		activatorProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		activatorProposalAdapter.setLabelProvider(new ActivatorLabelProvider());
		
		// Listeners
		Listener markDirtyListener = new Listener() {
			public void handleEvent(Event event) {
				if(!inRefresh.get())
					markDirty();
			}
		};
		txtBSN.addListener(SWT.Modify, markDirtyListener);
		txtVersion.addListener(SWT.Modify, markDirtyListener);
		
		txtVersion.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				IMessageManager msgs = getManagedForm().getMessageManager();
				try {
					new Version(txtVersion.getText());
					msgs.removeMessage("ERROR_" + Constants.BUNDLE_VERSION, txtVersion);
				} catch (IllegalArgumentException x) {
					msgs.addMessage("ERROR_" + Constants.BUNDLE_VERSION, "Invalid version format.", null, IMessageProvider.ERROR, txtVersion);
				}
			}
		});
		
		// Layout
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		
		GridData gd;
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 150;
		gd.horizontalIndent = 5;
		txtBSN.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 150;
		gd.horizontalIndent = 5;
		txtVersion.setLayoutData(gd);
		
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 150;
		gd.horizontalIndent = 5;
		txtActivator.setLayoutData(gd);
	}
	
	@Override
	public void commit(boolean onSave) {
		// TODO Auto-generated method stub
		super.commit(onSave);
		
		String bsn = txtBSN.getText();
		if(bsn != null && bsn.length() == 0) bsn = null;
		model.setBundleSymbolicName(bsn);
		
		String version = txtVersion.getText();
		if(version != null && version.length() == 0) version = null;
		model.setBundleVersion(version);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		
		if(inRefresh.compareAndSet(false, true)) {
			try {
				String bsn = model.getBundleSymbolicName();
				txtBSN.setText(bsn != null ? bsn : ""); //$NON-NLS-1$
				
				String bundleVersion = model.getBundleVersionString();
				txtVersion.setText(bundleVersion != null ? bundleVersion : ""); //$NON-NLS-1$
			} finally {
				inRefresh.set(false);
			}
		}
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		this.model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(this);
		
		IMessageManager msgs = form.getMessageManager();
		
		msgs.addMessage("INFO_" + Constants.BUNDLE_SYMBOLICNAME, "The symbolic name of the bundle will default to the filename, without the .bnd extension.", null, IMessageProvider.INFORMATION, txtBSN);
	}
	
	@Override
	public boolean setFormInput(Object input) {
		super.setFormInput(input);
		
		if(this.model != null) {
			this.model.removePropertyChangeListener(this);
		}
		this.model = (BndEditModel) input;
		this.model.addPropertyChangeListener(this);
		return false;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		markStale();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if(this.model != null)
			this.model.removePropertyChangeListener(this);
	}
	
	private IProject getProject() {
		IProject project = null;
		
		IEditorInput editorInput = ((IFormPage) getManagedForm().getContainer()).getEditorInput();
		if(editorInput instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) editorInput).getFile();
			project = file.getProject();
		}
		
		return project;
	}
	
	private class ActivatorProposalProvider implements IContentProposalProvider {
		public IContentProposal[] getProposals(String contents, int position) {
			String substring = contents.substring(0, position).toLowerCase();
			
			final List<IContentProposal> proposals = new LinkedList<IContentProposal>();
			try {
				IJavaProject javaProject = JavaCore.create(getProject());
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
						IJavaSearchScope searchScope = javaProject != null ? SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject }) : null;
						if(searchScope != null) {
							new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), searchScope,
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
