package fll.fileserver.pages.files;

import fll.fileserver.Log;
import fll.fileserver.FileManager;


import fll.fileserver.html.HtmlFormatter;
import fll.fileserver.html.HtmlElement;
import fll.fileserver.HttpUtil;


import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.util.HashMap;

import java.net.InetSocketAddress;
import java.net.URI;


public class Files
implements HttpHandler
{
    private Log log;
    private FileManager fileMgr;

    private final String FILE_FORM_INPUT = "file";
    
    
    public Files(Log log, FileManager fileMgr)
    {
	this.log = log;
	this.fileMgr = fileMgr;
    }
    
    private void showDirectory(HttpExchange exchange,
    String userID,
    String[] URIComponents)
    {
	File[] fileList;

	Headers responseHeaders;
	OutputStream responseBody;
	HashMap<String, String> pageArgs;
	String page;


	fileList = fileMgr.listFiles(userID, "");

	pageArgs = new HashMap<String, String>();
	pageArgs.put("file-form-input", FILE_FORM_INPUT);

	try {
	    responseHeaders = exchange.getResponseHeaders();
	    responseBody = exchange.getResponseBody();
	    
	    page = HtmlFormatter.format(HtmlFormatter.readWholePageFile("pages/file-manager.temp.html"), pageArgs);

	    responseHeaders.set("content-type", "text/html");
	    exchange.sendResponseHeaders(200, page.length());
	    responseBody.write(page.getBytes());

	    responseBody.close();
	    exchange.close();
	} catch(IOException e) {
	    log.error(String.format("|pages.files.Files.handleUploadFile| IOException : %s", e.getMessage()));
	}
	
	return;
    }

    private void handleUploadFile(HttpExchange exchange,
				  String userID)
    {
	InputStream reqBody;
	int nb;
	String boundary;
	String headers;
	FileWriter targetFile;
	
	try {
	    
	    reqBody = exchange.getRequestBody();

	    boundary = HttpUtil.multipartReadBoundary(reqBody);
	    headers = HttpUtil.multipartReadHeaders(reqBody);

	    log.info(boundary);
	    log.info(headers);
	    
	    if(fileMgr.fileExists(userID, "a.txt")) {
		HttpUtil.badRequest(exchange);
		return;
	    }

	    targetFile = fileMgr.openNewFileWrite(userID, "a.txt");

	    HttpUtil.multipartReadIntoFile(reqBody, targetFile, boundary);
	    targetFile.close();
	    
	    // while(true) {
		// nb = reqBody.read();
		// if(nb == -1) { break; }
		// System.out.print((char)nb);
	    // }
	    // System.out.print("\n");
	} catch(IOException e) {
	    log.error(String.format("|pages.files.Files| IOException : %s", e.getMessage()));
	    return;
	}
	
	HttpUtil.OK(exchange);
    }

    private String parseAuthCookie(HttpExchange exchange)
    {
	String cookieStr;
	String[] cookies;
	String[] cookieI;
	

	cookieStr = exchange.getRequestHeaders().getFirst("Cookie");
	if(cookieStr == null) {
	    return null;
	}
	cookies = cookieStr.split(";");

	for(int i=0;i<cookies.length;i++) {
	    cookieI = cookies[i].split("=");
	    if(cookieI.length < 2) { continue; }
	    if(cookieI[0].equals("auth")) {
		return cookieI[1];
	    }
	}

	return null;
	
    }
    
    @Override
    public void handle(HttpExchange exchange)
    {
	String URI;
	String[] URIComponents;
	String auth;

	auth = parseAuthCookie(exchange);
	if(auth == null) {
	    HttpUtil.redirect(exchange, "/");
	}
	auth = HttpUtil.escapeInput(auth);
	
	URI = exchange.getRequestURI().getPath();
	URIComponents =  URI.split("/");

	if(exchange.getRequestMethod().equals("POST") && URIComponents.length >= 3) {
	    if(URIComponents[2].equals("upload")) {
		handleUploadFile(exchange, auth);
	    }
	    
	    return;
	}
	
	showDirectory(exchange, auth, URIComponents);

	return;
	

	// if(exchange.getRequestMethod().equals("POST") && URIComponents.length >= 3) {
	    
	// }
    }

    
}
