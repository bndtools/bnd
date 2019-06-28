package bndtools.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class CompositeTask implements IRunnableWithProgress {

	private final List<IRunnableWithProgress>	tasks		= new ArrayList<>();
	private final List<Integer>					weights		= new ArrayList<>();

	private int									totalWeight	= 0;

	public void addTask(IRunnableWithProgress task, int weight) {
		tasks.add(task);
		weights.add(weight);
		totalWeight += weight;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		SubMonitor progress = SubMonitor.convert(monitor, "Composite Task...", totalWeight);

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i)
				.run(progress.newChild(weights.get(i), SubMonitor.SUPPRESS_NONE));
			if (progress.isCanceled())
				return;
		}
	}

}
