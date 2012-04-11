package test.lib;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import aQute.lib.base64.Base64;

public class AuthNanoHTTPD extends NanoHTTPD {

	private static final String HTTP_HEADER_AUTHORIZATION = "authorization";
	private static final String PREFIX_BASIC_AUTH = "Basic ";
	
	private static final String HTTP_AUTHORIZATION_REQUIRED = "401 Authorization Required";
	
	private final String encodedAuthString;

	/**
	 * @param user The required user name
	 * @param pass The required password
	 * @throws IOException
	 */
	public AuthNanoHTTPD(int port, File wwwroot, String user, String pass) throws IOException {
		super(port, wwwroot);
		encodedAuthString = Base64.encodeBase64((user + ":" + pass).getBytes());
	}
	
	@Override
	public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
		String authHeader = header.getProperty(HTTP_HEADER_AUTHORIZATION);
		if (authHeader == null)
			return new Response(HTTP_AUTHORIZATION_REQUIRED, "text/plain", "No user/password provided");
		
		if (authHeader.startsWith(PREFIX_BASIC_AUTH))
			authHeader = authHeader.substring(PREFIX_BASIC_AUTH.length());
		else
			return new Response(HTTP_BADREQUEST, "text/plain", "Authorization type is not supported");
		
		if (!encodedAuthString.equals(authHeader))
			return new Response(HTTP_AUTHORIZATION_REQUIRED, "text/plain", "Invalid user/password");
		
		return super.serve(uri, method, header, parms, files);
	}

}
