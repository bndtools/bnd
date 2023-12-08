package biz.aQute.bnd.lsp.provider;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

public class BNDWorkspaceService implements WorkspaceService {

	BNDWorkspaceService() {
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		// TODO Auto-generated method stub
		System.out.println("BNDWorkspaceService.didChangeConfiguration()");
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		// TODO Auto-generated method stub
		System.out.println("BNDWorkspaceService.didChangeWatchedFiles()");
	}

}
