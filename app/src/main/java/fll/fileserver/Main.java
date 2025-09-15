package fll.fileserver;

import fll.fileserver.Log;
import fll.fileserver.Database;
import fll.fileserver.FileManager;

import fll.fileserver.pages.login.Login;
import fll.fileserver.pages.admin.Admin;
import fll.fileserver.pages.files.Files;

import fll.fileserver.testing.Stop;



import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Main
{

    public static void main(String[] args)
    {
	Log log = new Log(Log.defaultLogPath());
	FileManager fileMgr = new FileManager(log, "files");
	Database db = new Database("database/user.db", log, fileMgr);
	
	
	
	try {
	    HttpServer srv = HttpServer.create(new InetSocketAddress(8080), 0);
	    srv.createContext("/admin", new Admin(log, db, fileMgr));
	    srv.createContext("/files" , new Files(log, fileMgr));
	    srv.createContext("/stop" , new Stop(log, db));
	    srv.createContext("/", new Login(log, db));
	    
	    
	    srv.setExecutor(null);
	    srv.start();
	    log.info("server started");
	} catch (IOException e) {
	    System.out.println("error starting server: " + e.getMessage());

	}
	
    }
}

