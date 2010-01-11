package name.neilbartlett.eclipse.bndtools.editor.components;

import java.text.MessageFormat;

import name.neilbartlett.eclipse.bndtools.utils.JavaContentProposalLabelProvider;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SvcInterfaceSelectionDialog extends InputDialog {
	
	private static final String AUTO_ACTIVATE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890._"; //$NON-NLS-1$
	
	private final IJavaProject javaProject;
	private final IRunnableContext context;
	
	public SvcInterfaceSelectionDialog(Shell parentShell, String dialogTitle, String dialogMessage, IJavaProject javaProject, IRunnableContext context) {
		super(parentShell, dialogTitle, dialogMessage, "", null);
		this.javaProject = javaProject;
		this.context = context;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control dialogArea = super.createDialogArea(parent);
		
		FieldDecoration proposalDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);

		KeyStroke assistKeyStroke = null;
		try {
			assistKeyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}
		
		Text textField = getText();
		ControlDecoration decor = new ControlDecoration(textField, SWT.LEFT | SWT.TOP);
		decor.setImage(proposalDecoration.getImage());
		decor.setDescriptionText(MessageFormat.format("Content Assist is available. Press {0} or start typing to activate", assistKeyStroke.format()));
		decor.setShowHover(true);
		decor.setShowOnlyOnFocus(true);
		
		IContentProposalProvider proposalProvider = new SvcInterfaceProposalProvider(javaProject, context);
		
		ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(textField, new TextContentAdapter(), proposalProvider, assistKeyStroke, AUTO_ACTIVATE_CHARS.toCharArray());
		proposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		proposalAdapter.setLabelProvider(new JavaContentProposalLabelProvider());
		proposalAdapter.setAutoActivationDelay(1500);
		
		return dialogArea;
	}
}
