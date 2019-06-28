package org.bndtools.core.resolve;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import biz.aQute.resolve.ResolutionCallback;
import bndtools.Plugin;

public class ResolveJob extends Job {

	private final BndEditModel				model;
	private final IResource					inputResource;
	private final List<ResolutionCallback>	callbacks	= new LinkedList<>();

	private ResolutionResult				result;

	public ResolveJob(BndEditModel model, IResource inputResource) {
		super("Resolving " + model.getBndResourceName());
		this.model = model;
		this.inputResource = inputResource;
	}

	public IStatus validateBeforeRun() {
		try {

			//
			// The BndEdit model does not do property expansion. So
			// get the processor to get the expansions.
			//

			Processor p = model.getProperties();

			String runfw = p.getProperty(Constants.RUNFW);
			if (runfw == null)
				return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					Messages.ResolutionJob_errorFrameworkOrExecutionEnvironmentUnspecified, null);

			try {
				if (Arrays.stream(inputResource.getProject()
					.getDescription()
					.getNatureIds())
					.anyMatch(natureId -> "org.eclipse.m2e.core.maven2Nature".equals(natureId))) {

					return Status.OK_STATUS;
				}
			} catch (Exception e) {
				// ignore this
			}

			String eeStr = p.getProperty(Constants.RUNEE);
			if (eeStr == null)
				return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					Messages.ResolutionJob_errorFrameworkOrExecutionEnvironmentUnspecified, null);

			EE ee = EE.parse(eeStr);
			if (ee == null) {
				String supportedEEs = Arrays.stream(EE.values())
					.map(EE::getEEName)
					.collect(Collectors.joining(",\n - "));
				return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					MessageFormat.format(
						"Unrecognized Execution Environment: \"{0}\".\n\nSupported values are:\n - {1}", eeStr,
						supportedEEs),
					null);
			}

			return Status.OK_STATUS;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ResolveOperation operation = new ResolveOperation(model, callbacks);
		operation.run(monitor);
		result = operation.getResult();

		return Status.OK_STATUS;
	}

	public ResolutionResult getResolutionResult() {
		return result;
	}

	public void addCallback(ResolutionCallback callback) {
		callbacks.add(callback);
	}

}
