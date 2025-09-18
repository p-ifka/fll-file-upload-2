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
import java.io.FileReader;
import java.io.FileNotFoundException;
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

	String fileDisplayTemp;
	HashMap<String,String> fileDisplayTempArgs;
	String fileListDisplay = "";
	String page;
	

	fileList = fileMgr.listFiles(userID, "");
	fileDisplayTemp = HtmlFormatter.readWholePageFile("elements/file.temp.html");
	fileDisplayTempArgs = new HashMap<String,String>();
	for(int i=0;i<fileList.length;i++) {
	    fileDisplayTempArgs.put("file-name", fileList[i].getName());
	    fileDisplayTempArgs.put("file-download-link", String.format("download/%s", userID, fileList[i].getName()));
	    fileListDisplay = fileListDisplay + HtmlFormatter.format(fileDisplayTemp, fileDisplayTempArgs);
	}
	

	pageArgs = new HashMap<String, String>();
	pageArgs.put("css", HtmlFormatter.readWholePageFile("pages/theme.css"));
	pageArgs.put("file-form-input", FILE_FORM_INPUT);
	pageArgs.put("file-list", fileListDisplay);


	

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

    private boolean isLegalFilePath(String filename)
    /**
    currently just make sure there is no ../ in filename

    @param String filename : filename parameter of request

    @return true if filename can be accepted, false if not
    **/
    {
	String[] path;

	path = filename.split("/");

	for(int i=0;i<path.length;i++) {
	    if(path[i].equals("..")) {
		return false;
	    }
	}

	return true;
    }
    private void handleDownloadFile(HttpExchange exchange,
    String userID,
    String[] URI)
    {
	int nb, i;
	Headers responseHeaders;
	OutputStream responseBody;
	FileReader freader;
	

	if(URI.length < 4) {
	    HttpUtil.badRequest(exchange);
	}
	
	try {
	    freader = fileMgr.openFileRead(userID, URI[3]);
	} catch(FileNotFoundException e) {
	    HttpUtil.notFound(exchange);
	    return;
	}
	
	try {
	    responseHeaders = exchange.getResponseHeaders();
	    responseBody = exchange.getResponseBody();


	    for(i=0;true;i++) {
		nb = freader.read();
		if(nb == -1) {
		    break;
		}
		responseBody.write((char)nb);
	    }
	    responseHeaders.set("content-type", "text/plain");
	    responseHeaders.set("content-disposition", "attachment");
	    responseHeaders.set("filename", URI[3]);
	    exchange.sendResponseHeaders(200, i);

	    freader.close();
	    responseBody.close();
	    exchange.close();
	} catch(IOException e) {
	    log.error(String.format("|pages.files.Files.handleDownloadFile| IOException : %s", e.getMessage()));
	}

	
    }
    
    private void handleUploadFile(HttpExchange exchange,
    String userID)
    {
	log.info("uploading file");

	InputStream reqBody;
	String boundary;
	String headers;

	int filenameStart;
	int filenameEnd;
	String filename;
	FileWriter targetFile;


	try {

	    reqBody = exchange.getRequestBody();

	    boundary = HttpUtil.multipartReadBoundary(reqBody);
	    headers = HttpUtil.multipartReadHeaders(reqBody);

	    filenameStart = headers.indexOf("filename=");
	    if(filenameStart < 0) {
		HttpUtil.badRequest(exchange);
		return;
	    }
	    filenameStart += 10; // move forward past 'filename="'

	    filenameEnd = headers.indexOf('\"', filenameStart);
	    if(filenameEnd < 0) {
		HttpUtil.badRequest(exchange);
		return;
	    }

	    filename = headers.substring(filenameStart, filenameEnd);
	    if(!isLegalFilePath(filename)) {
		HttpUtil.badRequest(exchange);
		return;
	    }
	    // log.info(boundary);
	    // log.info(headers);

	    if(fileMgr.fileExists(userID, filename)) {
		HttpUtil.badRequest(exchange);
		return;
		}

		targetFile = fileMgr.openNewFileWrite(userID, filename);

		log.info("opened file");
		HttpUtil.multipartReadIntoFile(reqBody, targetFile, boundary);
		targetFile.close();

		HttpUtil.redirectSeeOther(exchange, "/files");

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
		    return;
		}
		auth = HttpUtil.unescapeInput(auth);

		URI = exchange.getRequestURI().getPath();
		URIComponents =  URI.split("/");
		log.info(URI);

		if(exchange.getRequestMethod().equals("POST") && URIComponents.length >= 3) {
		    switch(URIComponents[2]) {
		    case "upload":
			handleUploadFile(exchange, auth);
			break;
		    case "download":
			handleDownloadFile(exchange, auth, URIComponents);
			break;
		    default:
			HttpUtil.notFound(exchange);
			break;
		    }
		    return;
		}

		showDirectory(exchange, auth, URIComponents);

		return;


		// if(exchange.getRequestMethod().equals("POST") && URIComponents.length >= 3) {

		    // }
		}


	    }
