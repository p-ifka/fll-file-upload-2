package fll.fileserver.pages.login;

import fll.fileserver.html.HtmlElement;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;


public class Login
    implements HttpHandler
{

    private String htmlPage = "<!DOCTYPE html>";
	

    
    @Override
    public void handle(HttpExchange exchange)
	throws IOException
    {

	HtmlElement body = new HtmlElement("body");
	HtmlElement text = new HtmlElement("p");

	text.setContents("text");
	body.setContents(text.toElementString());

	htmlPage = htmlPage + body.toElementString();
	
	
	Headers responseHeaders = exchange.getResponseHeaders();
	responseHeaders.set("content-type", "text/html");
	exchange.sendResponseHeaders(200, htmlPage.length());

	OutputStream outStream = exchange.getResponseBody();
	outStream.write(htmlPage.getBytes());
	outStream.close();
	
    }
    
}
