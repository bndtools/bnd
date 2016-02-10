package biz.aQute.http.testservers;

import java.io.IOException;

import biz.aQute.http.testservers.HttpTestServer.Request;
import biz.aQute.http.testservers.HttpTestServer.Response;

public interface HttpHandler {

	void handle(String remainder, Request request, Response response) throws IOException;

}
