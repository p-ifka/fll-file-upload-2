package fll.fileserver.testing;


import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;



import fll.fileserver.Database;
import fll.fileserver.Log;


public class Stop
    implements HttpHandler
{
    private Log logInst;
    private Database dbInst;


    public Stop(Log logInstance, Database databaseInstance)
    {
	logInst = logInstance;
	dbInst = databaseInstance;
	
    }

    @Override
    public void handle(HttpExchange exchange)
    {
	logInst.info("stopping server");

	dbInst.close();
	logInst.close();
	
	try {
	    exchange.sendResponseHeaders(200, 4);

	    OutputStream outStream = exchange.getResponseBody();
	    outStream.write("stop".getBytes());
	    outStream.close();
	} catch(IOException e) {
	    System.exit(1);
	}

	System.exit(0);


    }


}
