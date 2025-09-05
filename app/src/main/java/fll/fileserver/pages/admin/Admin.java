package fll.fileserver.pages.admin;

import fll.fileserver.Database;
import fll.fileserver.Log;
import fll.fileserver.html.HtmlFormatter;
import fll.fileserver.html.HtmlElement;
import fll.fileserver.HttpUtil;


import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.util.HashMap;

import java.net.InetSocketAddress;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Admin
implements HttpHandler
{
    private final String URI_ROOT = "/admin";
    private final String ADD_GROUP_FORM_NAME_INPUT = "new-group-name"; // 'name' attribute of <input> for the group name of a group to be added
    private final String ADD_GROUP_FORM_EXPIRY_INPUT = "new-group-expiry"; // 'name' attribute of <input> for the expiration date of a group to be added





    private Log logInst;
    private Database dbInst;


    public Admin(Log logInstance, Database databaseInstance)
    {
	logInst = logInstance;
	dbInst = databaseInstance;
    }

    String pageFileData() {
	String page = "";
	try {
	    File pageFile = new File("pages/admin.temp.html");
	    Scanner pageFileReader = new Scanner(pageFile);
	    while(pageFileReader.hasNextLine()) {
		page = page + pageFileReader.nextLine();
	    }
	    return page;
	} catch (FileNotFoundException e) {
	    logInst.error(String.format("file not found: %s", e.getMessage()));
	    System.exit(1);	// TODO: internal server error page here
	    return "";
	}

    }


    private void handleAddGroup(HttpExchange exchange)
    /**
    handle POST requests to /admin/add-group

    @param exchange : HttpExchange to read parameters from and return result to
    **/
    {
	boolean hasExpiry = false;
	String newGroupName = new String();
	String newGroupExpiryDate = new String();
	


	// logInst.info("rq to /admin/add-group recieved");
	// logInst.info(exchange.getRequestMethod());
	try {
	    InputStream reqBody = exchange.getRequestBody();

	    // String t = HttpUtil.postNextParameter(reqBody);
	    if(HttpUtil.postNextParameter(reqBody).equals(ADD_GROUP_FORM_NAME_INPUT)) {
		logInst.info((String.format("%s parameter found", ADD_GROUP_FORM_NAME_INPUT)));
		newGroupName = HttpUtil.postParameterValue(reqBody);
		if(newGroupName == null) {
		    HttpUtil.badRequest(exchange);
		}

		if(HttpUtil.postNextParameter(reqBody).equals(ADD_GROUP_FORM_EXPIRY_INPUT)) {
		    newGroupExpiryDate = HttpUtil.postParameterValue(reqBody);
		    logInst.info(newGroupExpiryDate);
		    if(newGroupExpiryDate != null) {
			hasExpiry = true;
		    }
		}
		
		int rc = Database.DB_SUCCESS;
		if(hasExpiry) {
		    logInst.info(String.format("new group expiry: %s", newGroupExpiryDate));
		    rc = dbInst.createGroup(newGroupName, newGroupExpiryDate);
		} else {
		    rc = dbInst.createGroup(newGroupName, Database.DB_EXPIRY_NONE);
		}

		if(rc == Database.DB_SUCCESS) {
		    HttpUtil.OK(exchange);
		} else {
		    HttpUtil.badRequest(exchange);
		}
		
		
		// if(HttpUtil.postNextParameter(reqBody) == ADD_GROUP_FORM_EXPIRY_INPUT) {
		    // String newGroupExpiry = HttpUtil.postNextParameter(reqBody);
		    // logInst.info(String.format("creating new group with expiry: %s", newGroupExpiry));
		} else {	// no group name parameter: ignore request
		HttpUtil.badRequest(exchange);
	    }

	    // logInst.info(("returning OK"));
	    // HttpUtil.OK(exchange);
	} catch(IOException e) {
	    logInst.error(String.format("pages.admin.Admin.handleAddGroup IOException : %s", e.getMessage()));
	    System.exit(1);
	}
    }



    @Override
    public void handle(HttpExchange exchange)
    {
	logInst.info("request to /admin/");
	String URI = exchange.getRequestURI().getPath();

	switch(URI) {
	case "/admin/add-group":
	    handleAddGroup(exchange);
	    return;
	    // break;		// sure           // well nevermind



	}

	String groupTable = "";


	try {
	    Database.SqlQuery getGroupsQuery = dbInst.getGroups();
	    ResultSet groups = getGroupsQuery.result;

	    while(groups.next()) {
		HtmlElement row = new HtmlElement("tr");
		row.setContents(String.format("<td>%s</td><td>%s</td>", groups.getString(1), groups.getString(2)));
		groupTable = groupTable + row.toElementString();
	    }

	    getGroupsQuery.free();
	    
	} catch(SQLException e) {
	    logInst.error(String.format("sql error: %s", e.getMessage()));
	    System.exit(1);	// TODO: internal server error page here
	}

	HashMap<String, String> args = new HashMap<String,String>();

	args.put("add_group_form_name_input", ADD_GROUP_FORM_NAME_INPUT);
	args.put("add_group_form_expiry_input", ADD_GROUP_FORM_EXPIRY_INPUT);
	args.put("table-contents", groupTable);

	String page = HtmlFormatter.format(pageFileData(), args);
	try {

	    Headers responseHeaders = exchange.getResponseHeaders();
	    responseHeaders.set("content-type", "text/html");
	    exchange.sendResponseHeaders(200, page.length());


	    OutputStream outStream = exchange.getResponseBody();
	    outStream.write(page.getBytes());

	    outStream.close();
	    exchange.close();
	} catch(IOException e) {
	    logInst.error(String.format("io exception: %s", e.getMessage()));
	    System.exit(1);
	}

    }

}
