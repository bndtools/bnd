package test.http;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

public class ETaggingResourceHandler extends ResourceHandler {

	MimeTypes _mimeTypes = new MimeTypes();

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {
		if (baseRequest.isHandled())
			return;

		boolean skipContentBody = false;

		if (!HttpMethods.GET.equals(request.getMethod())) {
			if (!HttpMethods.HEAD.equals(request.getMethod())) {
				// try another handler
				super.handle(target, baseRequest, request, response);
				return;
			}
			skipContentBody = true;
		}

		Resource resource = getResource(request);

		if (resource == null || !resource.exists()) {
			// no resource - try other handlers
			super.handle(target, baseRequest, request, response);
			return;
		}

		// We are going to serve something
		baseRequest.setHandled(true);

		if (resource.isDirectory()) {
			response.sendError(HttpStatus.FORBIDDEN_403);
		}

		// set some headers
		long last_modified = resource.lastModified();
		if (last_modified > 0) {
			long if_modified = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
			if (if_modified > 0 && last_modified / 1000 <= if_modified / 1000) {
				response.setStatus(HttpStatus.NOT_MODIFIED_304);
				return;
			}
		}

		String etag = calculateETag(resource);
		String incomingETag = request.getHeader(HttpHeaders.IF_NONE_MATCH);
		if (incomingETag != null) {
			if (incomingETag.equals(etag)) {
				response.setStatus(HttpStatus.NOT_MODIFIED_304);
				return;
			}
		}
		response.setHeader(HttpHeaders.ETAG, etag);

		Buffer mime = _mimeTypes.getMimeByExtension(resource.toString());
		if (mime == null)
			mime = _mimeTypes.getMimeByExtension(request.getPathInfo());

		// set the headers
		doResponseHeaders(response, resource, mime != null ? mime.toString() : null);
		response.setDateHeader(HttpHeaders.LAST_MODIFIED, last_modified);
		if (skipContentBody)
			return;

		// Send the content
		OutputStream out = null;
		try {
			out = response.getOutputStream();
		} catch (IllegalStateException e) {
			out = new WriterOutputStream(response.getWriter());
		}
		resource.writeTo(out, 0, resource.length());
	}

	protected static String calculateETag(Resource resource) {
		return Integer.toHexString(Long.toString(resource.length())
			.hashCode());
	}

}
