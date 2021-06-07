package org.bndtools.core.resolve.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;

import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.core.resolve.ResolveOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.exceptions.Exceptions;
import bndtools.Plugin;

public class ResolutionResultsWizardPage extends WizardPage implements ResolutionResultPresenter {

	public static final String				PROP_RESULT				= "result";

	private final BndEditModel				model;
	private final PropertyChangeSupport		propertySupport			= new PropertyChangeSupport(this);

	private final ResolutionSuccessPanel	resolutionSuccessPanel;
	private final ResolutionFailurePanel	resolutionFailurePanel	= new ResolutionFailurePanel();
	private final ResolutionCanceledPanel	resolutionCanceledPanel	= new ResolutionCanceledPanel();

	private StackLayout						stack;

	private ResolutionResult				result;
	private boolean							allowCompleteUnresolved	= false;

	/**
	 * Create the wizard.
	 */
	public ResolutionResultsWizardPage(BndEditModel model) {
		super("resultsPage");
		this.model = model;
		setTitle("Resolution Results");
		setDescription(
			"The required resources will be used to create the Run Bundles list. NOTE: The existing content of Run Bundles will be replaced!");

		resolutionSuccessPanel = new ResolutionSuccessPanel(model, this);
	}

	/**
	 * Create contents of the wizard.
	 *
	 * @param parent
	 */
	@Override
	public void createControl(Composite parent) {
		parent.setBackground(parent.getDisplay()
			.getSystemColor(SWT.COLOR_WHITE));
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		stack = new StackLayout();
		container.setLayout(stack);

		resolutionSuccessPanel.createControl(container);
		resolutionFailurePanel.createControl(container);
		resolutionCanceledPanel.createControl(container);

		updateUi();
	}

	public ResolutionResult getResult() {
		return result;
	}

	public void setResult(ResolutionResult result) {
		ResolutionResult oldValue = this.result;
		this.result = result;
		propertySupport.firePropertyChange(PROP_RESULT, oldValue, result);
		if (getControl() != null && !getControl().isDisposed())
			updateUi();
	}

	@Override
	public void recalculate() {
		try {
			result.getLogger()
				.close();
			ResolveOperation resolver = new ResolveOperation(model);
			getContainer().run(true, true, resolver);
			setResult(resolver.getResult());
		} catch (InvocationTargetException e) {
			ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
				"Unexpected error", Exceptions.unrollCause(e, InvocationTargetException.class)));
			setResult(null);
		} catch (InterruptedException e) {
			setResult(null);
		} finally {
			updateUi();
		}
	}

	private void updateUi() {
		// String log = (result != null) ? result.getLog() : null;
		// txtLog.setText(log != null ? log : "<<UNAVAILABLE>>");
		ResolutionResult.Outcome outcome = result != null ? result.getOutcome() : ResolutionResult.Outcome.Unresolved;
		// SWTUtil.recurseEnable(resolved, tbtmResults.getControl());
		// SWTUtil.recurseEnable(!resolved, tbtmErrors.getControl());
		switch (outcome) {
			case Resolved :
				resolutionSuccessPanel.setInput(result);
				stack.topControl = resolutionSuccessPanel.getControl();
				break;
			case Cancelled :
				resolutionCanceledPanel.setInput(result);
				setErrorMessage("Resolution canceled!");
				stack.topControl = resolutionCanceledPanel.getControl();
				break;
			default :
				resolutionFailurePanel.setInput(result);
				setErrorMessage("Resolution failed!");
				stack.topControl = resolutionFailurePanel.getControl();
				break;
		}
		// stack.topControl = resolutionSuccessPanel.getControl();
		((Composite) getControl()).layout(true, true);

		updateButtons();
	}

	@Override
	public void updateButtons() {
		getContainer().updateButtons();
	}

	@Override
	public boolean isPageComplete() {
		boolean complete;
		if (allowCompleteUnresolved)
			complete = true;
		else
			complete = result != null && result.getOutcome()
				.equals(ResolutionResult.Outcome.Resolved) && resolutionSuccessPanel.isComplete();
		return complete;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName, listener);
	}

	@Override
	public void dispose() {
		super.dispose();

		resolutionFailurePanel.dispose();
		resolutionSuccessPanel.dispose();
		resolutionCanceledPanel.dispose();

	}

	public void setAllowCompleteUnresolved(boolean allowFinishUnresolved) {
		this.allowCompleteUnresolved = allowFinishUnresolved;
	}

}
