package fll.fileserver.pages.login;

import fll.fileserver.Log;
import fll.fileserver.html.HtmlElement;
import fll.fileserver.html.HtmlFormatter;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;

import java.util.HashMap;

public class Login
    implements HttpHandler
{

    private int count = 0;
    private Log log;


    public Login(Log logInstance)
    {
	log = logInstance;
    }

    

    @Override
    public void handle(HttpExchange exchange)
	throws IOException
    {
	log.info("Login: recieved request");
	
	count++;

	String page;
	HashMap<String, String> args = new HashMap<String, String>();
	args.put("count", String.valueOf(count));

	page = HtmlFormatter.format(""
				    + "<!DOCTYPE html>"
				    + "<head><title>test page</title></head>"
				    + "<body><p>you are the ~count~th visit ~~~~</p></body>"
				    + "",
				    args);




	// HashMap<String, String> args = new HashMap<String, String>();
	// args.put("name", "foo");
	// args.put("pass", "bar");
	// String fmt = "user: ~name~\n~~password ~pass~";

	// String data = HtmlFormatter.format(fmt, args);



	// HtmlElement body = new HtmlElement("body");
	// HtmlElement text = new HtmlElement("p");

	// text.setContents(data);
	// body.setContents(text.toElementString());

	// htmlPage = htmlPage + body.toElementString();


	Headers responseHeaders = exchange.getResponseHeaders();
	responseHeaders.set("content-type", "text/html");
	exchange.sendResponseHeaders(200, page.length());

	OutputStream outStream = exchange.getResponseBody();
	outStream.write(page.getBytes());
	outStream.close();

    }

}
