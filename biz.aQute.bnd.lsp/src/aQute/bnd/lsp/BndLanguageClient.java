package aQute.bnd.lsp;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;

import aQute.bnd.lsp.models.BndBuildStatus;

/**
 * The standard language client plus the notifications that only the bnd
 * language server sends.
 */
public interface BndLanguageClient extends LanguageClient {

	@JsonNotification(Constants.NOTIFICATION_BUILD_STATUS)
	void buildStatus(BndBuildStatus status);
}
