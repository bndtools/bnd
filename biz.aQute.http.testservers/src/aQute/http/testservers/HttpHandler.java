package aQute.http.testservers;

import java.io.IOException;

import aQute.http.testservers.HttpTestServer.Request;
import aQute.http.testservers.HttpTestServer.Response;

public interface HttpHandler {

	void handle(String remainder, Request request, Response response) throws IOException;

}
