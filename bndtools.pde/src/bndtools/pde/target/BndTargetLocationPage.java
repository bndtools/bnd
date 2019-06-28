package bndtools.pde.target;

import java.util.Collection;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.elements.TreeContentProvider;
import org.eclipse.pde.internal.ui.shared.target.IEditBundleContainerPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public abstract class BndTargetLocationPage extends WizardPage implements IEditBundleContainerPage {
	private final ILogger			logger;
	private final String			message;
	private final ITargetDefinition	targetDefinition;
	private final Image				bundleIcon;

	public BndTargetLocationPage(String pageName, String title, String message, ITargetDefinition targetDefinition) {
		super(pageName);

		setTitle(title);
		setMessage(message);

		this.logger = Logger.getLogger(getClass());
		this.message = message;
		this.targetDefinition = targetDefinition;
		this.bundleIcon = AbstractUIPlugin.imageDescriptorFromPlugin("bndtools.core", "/icons/bundle.png")
			.createImage();
	}

	public ITargetDefinition getTargetDefinition() {
		return targetDefinition;
	}

	protected void logError(String message, Exception e) {
		logger.logError(message, e);
		setMessage(message, IMessageProvider.ERROR);
	}

	protected void logWarning(String message, Exception e) {
		logger.logWarning(message, e);
		setMessage(message, IMessageProvider.WARNING);
	}

	protected void resetMessage() {
		setMessage(message);
	}

	protected TreeViewer createBundleListArea(Composite composite, int hSpan) {
		TreeViewer bundleList = new TreeViewer(
			new Tree(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER));
		bundleList.getTree()
			.setLayoutData(fillGridData(hSpan));
		bundleList.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return bundleIcon;
			}
		});
		bundleList.setContentProvider(new TreeContentProvider() {
			@Override
			public Object[] getElements(Object element) {
				return ((Collection<?>) element).toArray();
			}
		});
		return bundleList;
	}

	protected Object fillGridData(int hSpan) {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = hSpan;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		return gridData;
	}

	@Override
	public void storeSettings() {}
}
