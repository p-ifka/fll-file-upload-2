package fll.fileserver.pages.login;

import fll.fileserver.Log;
import fll.fileserver.Database;
import fll.fileserver.HttpUtil;
import fll.fileserver.html.HtmlElement;
import fll.fileserver.html.HtmlFormatter;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.net.InetSocketAddress;

import java.util.HashMap;
import java.util.Base64;

public class Login
implements HttpHandler
{

    private Log log;
    private Database database;

    private final String PASSWORD_FORM_INPUT = "password";

    public Login(Log logInstance, Database databaseInstance)
    {
	log = logInstance;
	database = databaseInstance;
    }


    private void setAuthCookie(HttpExchange exchange,
    String authPass)
    throws IOException
    {
	Headers responseHeaders;

	responseHeaders = exchange.getResponseHeaders();
	responseHeaders.set("set-cookie", String.format("auth=%s", HttpUtil.escapeOutput(authPass)));
    }



    private void handlePasswordSubmit(HttpExchange exchange)
    /**
    handle POST requests to /login/submit, check body for PASSWORD_INPUT and check if it matches
    a group in the database, if it does
    **/
    {
	InputStream reqBody;
	String pass;
	int authrc;
	try {
	    reqBody = exchange.getRequestBody();
	    if(HttpUtil.postNextParameter(reqBody).equals(PASSWORD_FORM_INPUT)) {
		pass = HttpUtil.postParameterValue(reqBody);
		if(pass == null) { HttpUtil.badRequest(exchange); }

		authrc = database.authenticateAny(pass);
		switch(authrc) {
		case Database.DB_ERROR:
		    showLoginPage(exchange, true, "couldn't authenticate : database error");
		    return;
		case Database.DB_ISSUE:
		    showLoginPage(exchange, true, "couldn't authenticate : incorrect password");
		    return;
		case Database.DB_SUCCESS:
		    setAuthCookie(exchange, pass);
		    showLoginPage(exchange, false, null);
		    return;
		default:
		    showLoginPage(exchange, true, "couldn't authenticate : something is very wrong");
		}

		// HttpUtil.textResponse(exchange, 200, HttpUtil.postParameterValue(reqBody));
	    } else {
		HttpUtil.badRequest(exchange);
	    }

	} catch(IOException e) {
	    log.error(String.format("|pages.login.Login.handlePasswordSubmit| IOException : %s", e.getMessage()));
	    HttpUtil.internalServerErr(exchange);
	}
    }

    private void showLoginPage(HttpExchange exchange, boolean authFail, String failMessage)
    /**
    show login page

    @param boolean authFail : whether to indicate that the previous password was incorrect
    **/
    {
	HashMap<String,String> pageArgs;
	String page;

	Headers responseHeaders;
	OutputStream responseBody;


	pageArgs = new HashMap<String,String>();
	pageArgs.put("password-form-input", PASSWORD_FORM_INPUT);
	if(authFail) {
	    pageArgs.put("status", failMessage);
	} else {
	    pageArgs.put("status", "");
	}

	page = HtmlFormatter.format(HtmlFormatter.readWholePageFile("pages/user-login.temp.html"), pageArgs);

	try {
	    responseHeaders = exchange.getResponseHeaders();
	    responseBody = exchange.getResponseBody();

	    responseHeaders.set("content-type", "text/html");
	    exchange.sendResponseHeaders(200, page.length());

	    responseBody.write(page.getBytes());

	    responseBody.close();
	    exchange.close();
	} catch(IOException e) {
	    log.error(String.format("|pages.login.showLoginPage| IOException : %s", e.getMessage()));
	}



    }

    @Override
    public void handle(HttpExchange exchange)
    throws IOException
    {
	String URI = exchange.getRequestURI().getPath();
	String[] URIComponents = URI.split("/");
	if(exchange.getRequestMethod().equals("POST") && URIComponents.length >= 2) {
	    if(URIComponents[1].equals("login-submit")) {
		handlePasswordSubmit(exchange);
		return;
	    }
	}

	showLoginPage(exchange, false, null);
	return;


	// String page;
	// HashMap<String, String> args = new HashMap<String, String>();
	// args.put("count", String.valueOf(count));

	// page = HtmlFormatter.format(""
	// + "<!DOCTYPE html>"
	// + "<head><title>test page</title></head>"
	// + "<body><p>you are the ~count~th visit ~~~~</p></body>"
	// + "",
	// args);




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


	// Headers responseHeaders = exchange.getResponseHeaders();
	// responseHeaders.set("content-type", "text/html");
	// responseHeaders.set("set-cookie", "sid=123");
	// exchange.sendResponseHeaders(200, page.length());

	// OutputStream outStream = exchange.getResponseBody();
	// outStream.write(page.getBytes());
	// outStream.close();

    }

}
