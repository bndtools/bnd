package bndtools.utils;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

public abstract class PartAdapter implements IPartListener {

	@Override
	public void partActivated(IWorkbenchPart part) {}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {}

	@Override
	public void partClosed(IWorkbenchPart part) {}

	@Override
	public void partDeactivated(IWorkbenchPart part) {}

	@Override
	public void partOpened(IWorkbenchPart part) {}
}
