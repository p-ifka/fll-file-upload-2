package fll.fileserver;

import fll.fileserver.Log;

import fll.fileserver.pages.login.Login;



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
	Log log = new Log(Log.defaultLogPath()); // TODO: close log file when server is stopped
	log.info("nothing");
	
	try {
	    HttpServer srv = HttpServer.create(new InetSocketAddress(8080), 0);
	    srv.createContext("/", new Login(log));

	    srv.setExecutor(null);
	    srv.start();
	    log.info("server started");
	} catch (IOException e) {
	    System.out.println("error starting server: " + e.getMessage());

	}
	
    }
}

