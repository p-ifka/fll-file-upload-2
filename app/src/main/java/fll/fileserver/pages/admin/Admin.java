package fll.fileserver.pages.admin;

import fll.fileserver.Database;
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
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.util.HashMap;

import java.net.InetSocketAddress;
import java.net.URI;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Admin
implements HttpHandler
{
    // private final String URI_ROOT = "/admin";
    private final String GROUP_NAME_FORM_INPUT = "group-name"; // 'name' attribute of <input> for the group name in group modification operations
    private final String GROUP_EXPIRY_FORM_INPUT = "new-group-expiry"; // 'name' attribute of <input> for the expiration date of a group in group modification operations
    private final String QUANTITY_FORM_INPUT = "quantity";


    private Log logInst;
    private Database dbInst;
    private FileManager fileMgr;


    public Admin(Log logInstance, Database databaseInstance, fileMgr fileManager)
    {
	logInst = logInstance;
	dbInst = databaseInstance;
	fileMgr = fileManager;
    }




    private void handleAddGroup(HttpExchange exchange)
    /**
    handle POST request to /admin/add-group

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
	    if(HttpUtil.postNextParameter(reqBody).equals(GROUP_NAME_FORM_INPUT)) {
		// logInst.info((String.format("%s parameter found", GROUP_NAME_FORM_INPUT)));
		newGroupName = HttpUtil.postParameterValue(reqBody);
		if(newGroupName == null) {
		    HttpUtil.badRequest(exchange);
		    return;
		}

		if(HttpUtil.postNextParameter(reqBody).equals(GROUP_EXPIRY_FORM_INPUT)) {
		    newGroupExpiryDate = HttpUtil.postParameterValue(reqBody);
		    logInst.info(newGroupExpiryDate);
		    if(newGroupExpiryDate != null) {
			hasExpiry = true;
		    }
		}

		int rc = Database.DB_SUCCESS;
		if(hasExpiry) {
		    // logInst.info(String.format("new group expiry: %s", newGroupExpiryDate));
		    rc = dbInst.createGroup(newGroupName, newGroupExpiryDate);
		} else {
		    rc = dbInst.createGroup(newGroupName, Database.DB_EXPIRY_NONE);
		}

		if(rc != Database.DB_SUCCESS) {
		    HttpUtil.badRequest(exchange);
		}
		
		rc = fileMgr.createGroupDir(newGroupName);

		if(rc != FileManager.FS_SUCCESS) {
		    
		}


		// if(HttpUtil.postNextParameter(reqBody) == GROUP_EXPIRT_FORM_INPUT) {
		    // String newGroupExpiry = HttpUtil.postNextParameter(reqBody);
		    // logInst.info(String.format("creating new group with expiry: %s", newGroupExpiry));
		} else {	// no group name parameter: ignore request
		HttpUtil.badRequest(exchange);
		return;
	    }

	    // logInst.info(("returning OK"));
	    // HttpUtil.OK(exchange);
	} catch(IOException e) {
	    logInst.error(String.format("|pages.admin.Admin.handleAddGroup| IOException : %s", e.getMessage()));
	    HttpUtil.internalServerErr(exchange);
	    return;
	}
    }


    private void handleAddUsers(HttpExchange exchange)
    /**
    handle POST request to /admin/add-users/

    @param exchange : HttpExchange to read parameters from and return result to
    **/
    {
	String groupName;
	String quantityStr;
	int quantity;
	int sqlRc;


	try {
	    InputStream reqBody = exchange.getRequestBody();

	    if(HttpUtil.postNextParameter(reqBody).equals(GROUP_NAME_FORM_INPUT)) {
		groupName = HttpUtil.postParameterValue(reqBody);
		if(groupName == null) { HttpUtil.badRequest(exchange); return; }

	    } else {
		HttpUtil.badRequest(exchange);
		return;
	    }

	    if(HttpUtil.postNextParameter(reqBody).equals(QUANTITY_FORM_INPUT)) {
		quantityStr = HttpUtil.postParameterValue(reqBody);
		if(quantityStr == null) { HttpUtil.badRequest(exchange); return; }
		quantity = Integer.parseInt(quantityStr);
	    } else {
		HttpUtil.badRequest(exchange);
		return;
	    }

	    sqlRc = dbInst.createUsers(groupName, quantity);
	    if(sqlRc == Database.DB_SUCCESS) {
		HttpUtil.OK(exchange);
	    } else if(sqlRc == Database.DB_ISSUE) {
		HttpUtil.badRequest(exchange);
	    } else {
		HttpUtil.internalServerErr(exchange);
	    }

	} catch(IOException e) {
	    logInst.error(String.format("|pages.admin.Admin.handleAddUsers| IOException : %s", e.getMessage()));
	    HttpUtil.internalServerErr(exchange);
	    return;
	}

    }


    private void showGroup(HttpExchange exchange, String groupID)
    {
	try {
	    if(!dbInst.doesGroupExist(groupID)) {
		HttpUtil.notFound(exchange);
		return;
	    }
	    
	    Database.SqlQuery userListQuery;
	    int userTableBufferLen;
	    StringBuffer userTable;
	    HtmlElement userTableItem;
	    int i = 0;

	    String page;
	    HashMap<String,String> pageArgs;
	    Headers responseHeaders;
	    OutputStream responseBody;
	    

	    userListQuery = dbInst.getUsersInGroup(groupID);

	    userTableBufferLen = 2048;
	    userTable = new StringBuffer(userTableBufferLen);
	    
	    while(userListQuery.result.next()) {
		userTableItem = new HtmlElement("li");
		userTableItem.setContents(userListQuery.result.getString(1));

		if(i >= userTableBufferLen) {
		    userTableBufferLen = userTableBufferLen + 2048;
		    userTable.setLength(userTableBufferLen);
		}
		userTable.append(userTableItem.toElementString());
		i++;
	    }
	    userListQuery.free();
	    
	    pageArgs = new HashMap<String,String>();
	    pageArgs.put("group-name", groupID);
	    pageArgs.put("user-list", userTable.toString());
	    page = HtmlFormatter.format(HtmlFormatter.readWholePageFile("pages/admin-group.temp.html"), pageArgs);

	    try {
		responseHeaders = exchange.getResponseHeaders();
		responseBody = exchange.getResponseBody();

		responseHeaders.set("content-type", "text/html");
		exchange.sendResponseHeaders(200, page.length());

		responseBody.write(page.getBytes());

		responseBody.close();
		exchange.close();
		
		
	    } catch(IOException e) {
		logInst.error(String.format("|pages.admin.Admin.showGroup| IOException : %s", e.getMessage()));
		HttpUtil.internalServerErr(exchange);
		return;
	    }
	    
	    
	} catch(SQLException e) {
	    logInst.error(String.format("|pages.admin.Admin.showGroup| SQLException : %s", e.getMessage()));
	    HttpUtil.internalServerErr(exchange);
	    return;
	}
	
    }

    @Override
    public void handle(HttpExchange exchange)
    {
	logInst.info("request to /admin/");
	// URI reqURI = exchange.getRequestURI();

	String URI = exchange.getRequestURI().getPath();
	String[] URIComponents = URI.split("/");
	if(exchange.getRequestMethod().equals("POST") && URIComponents.length >= 3) {
	    logInst.info("POST request recieved");
	    switch(URIComponents[2]) {
	    case "add-group":
		handleAddGroup(exchange);
		return;
	    case "add-users":
		handleAddUsers(exchange);
		return;
	    }

	}

	if(URIComponents.length >= 4 && URIComponents[2].equals("group")) {
	    logInst.info("showing group " + URIComponents[3]);
	    showGroup(exchange, URIComponents[3]);
	    return;
	}



	logInst.info("displaying main page");
	String groupTable = "";


	try {
	    Database.SqlQuery getGroupsQuery = dbInst.getGroups();
	    ResultSet groups = getGroupsQuery.result;

	    while(groups.next()) {
		String gtGroupName = groups.getString(1);
		HtmlElement row = new HtmlElement("div");
		HashMap<String, String> divArgs = new HashMap<String, String>();

		divArgs.put("group-name", gtGroupName);
		divArgs.put("group-expiry", groups.getString(2));
		divArgs.put("group-name", gtGroupName);
		divArgs.put("group-user-quantity", String.format("%d", dbInst.getUserCount(gtGroupName)));
		divArgs.put("group-name-form-input", GROUP_NAME_FORM_INPUT);
		divArgs.put("quantity-form-input", QUANTITY_FORM_INPUT);

		row.setContents(HtmlFormatter.format(HtmlFormatter.readWholePageFile("elements/admin-group-data.temp.html"), divArgs));

		groupTable = groupTable + row.toElementString();
	    }

	    getGroupsQuery.free();

	} catch(SQLException e) {
	    logInst.error(String.format("sql error: %s", e.getMessage()));
	    System.exit(1);	// TODO: internal server error page here
	}

	HashMap<String, String> args = new HashMap<String,String>();

	args.put("group-name-form-input", GROUP_NAME_FORM_INPUT);
	args.put("group-expiry-input", GROUP_EXPIRY_FORM_INPUT);
	args.put("table-contents", groupTable);

	String page = HtmlFormatter.format(HtmlFormatter.readWholePageFile("pages/admin.temp.html"), args);
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
