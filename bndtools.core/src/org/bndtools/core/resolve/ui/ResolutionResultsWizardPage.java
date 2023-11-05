package org.bndtools.core.resolve.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.core.resolve.ResolveOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.version.VersionRange;
import biz.aQute.resolve.ResolveProcess;
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

				String message = createHelpfulResolutionFailedMessage(result);

				setErrorMessage("Resolution failed! " + message);

				stack.topControl = resolutionFailurePanel.getControl();
				break;
		}
		// stack.topControl = resolutionSuccessPanel.getControl();
		((Composite) getControl()).layout(true, true);

		updateButtons();
	}

	/**
	 * Try to produce a more human readable message which provides a hint which
	 * dependency might be missing.
	 *
	 * @param result2
	 * @return
	 */
	private static String createHelpfulResolutionFailedMessage(ResolutionResult result) {
		String message = "";
		ResolutionException resolutionException = result.getResolutionException();
		if (resolutionException != null && resolutionException.getUnresolvedRequirements() != null
			&& !resolutionException.getUnresolvedRequirements()
				.isEmpty()) {

			// last entry contains the info which could be most useful
			// to create an error message
			List<Requirement> causalChain = ResolveProcess.getCausalChain(resolutionException);
			if (!causalChain.isEmpty()) {
				Requirement lastEntry = causalChain.get(causalChain.size() - 1);
				String pck = (String) lastEntry.getAttributes()
					.get("osgi.wiring.package");

				if (pck != null) {
					String resourceName = lastEntry.getResource()
						.toString();

					String filter = lastEntry.getDirectives()
						.get("filter");
					VersionRange versionRange = filterToVersionRange(filter);

					String messagePartVersion = versionRange != null ? versionRange.toString() : "";

					// best effort to come up with a useful message
					message = "You are missing a dependency containing the package / capability: \"" + pck
						+ "\"";

					if (messagePartVersion != null) {
						message += " (in version " + messagePartVersion + ")";
					}

					message += ". This is required by " + resourceName + ").";
				}

				// TODO handle other cases / heuristics

			}
		}
		return message;
	}

	/**
	 * Extracts version strings from an ldap filter expression e.g.
	 *
	 * <pre>
	 * osgi.wiring.package: (&(osgi.wiring.package=some.package.foo)(version>=2.3.0)(!(version>=3.0.0)))
	 * </pre>
	 *
	 * TODO maybe there is a better place for this helper
	 *
	 * @param text The input string containing version information.
	 * @return An array of strings containing the extracted version conditions.
	 */
	public static VersionRange filterToVersionRange(String text) {

		if (text == null) {
			return null;
		}

		// Define the regex pattern with capturing groups
		Pattern pattern = Pattern.compile("\\(!?(version>=([^)]+))\\)");
		Matcher matcher = pattern.matcher(text);

		// Array to hold the matches
		String[] versions = new String[2];
		int count = 0;

		// Find the matches and extract the captured groups
		while (matcher.find() && count < 2) {
			versions[count] = matcher.group(2); // Captured group 2 contains the
												// version number
			count++;
		}

		// Return the extracted versions if two were found
		return count == 2 ? new VersionRange(versions[0], versions[1]) : null;
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
		resolutionFailurePanel.dispose();
		resolutionSuccessPanel.dispose();
		resolutionCanceledPanel.dispose();
		super.dispose();
	}

	public void setAllowCompleteUnresolved(boolean allowFinishUnresolved) {
		this.allowCompleteUnresolved = allowFinishUnresolved;
	}

}
