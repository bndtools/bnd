package biz.aQute.demo.subsys.provider;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {
		rsp.getWriter()
			.println("Hello World");
	}
}
