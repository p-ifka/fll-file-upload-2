package fll.fileserver;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpUtil
{
    public static String postNextParameter(InputStream body)
    throws IOException
    {
	// System.out.println("getting next parameter");
	StringBuffer parameterName = new StringBuffer(255);
	int bufferSize = 255;
	int nextb;
	// System.out.println("buffer allocated");

	for(int i=0;true;i++) {
	    nextb = body.read();
	    // System.out.print((char)nextb);
	    if(nextb == -1) {
		break;
	    } else if((char)nextb == '=') {
		// System.out.println("end found");
		return  parameterName.toString();
		// String t = parameterName.toString();
		// System.out.println(t);
		// return t;
	    } else {
		if(i >= bufferSize) {
		    return null; // probably safe to ignore a parameter with this long of a name
		    // bufferSize = bufferSize + 255;
		    // parameterName.setLength(bufferSize);
		}
		parameterName.append((char)nextb);
	    }
	}
	return null;
    }




    public static String postParameterValue(InputStream body)
    throws IOException
    /**
    read parameter value from InputStream, stop at EOF or '&'.

    @param InputStream body : InputStream for post request body

    @return String with parameter value
    **/
    {
	int bufferSize = 255;
	StringBuffer parameterValue = new StringBuffer(bufferSize);
	int nextb;
	for(int i=0;true;i++) {
	    nextb = body.read();
	    if(nextb == -1) {
		if(i <= 0) {
		    return null;
		} else {
		    return parameterValue.toString();
		}
	    } else if((char)nextb == '&') {
		return parameterValue.toString();
	    } else {
		if(i >= bufferSize) {
		    return null;
		    // bufferSize = bufferSize + 255;
		    // parameterValue.setLength(bufferSize);
		}
		parameterValue.append((char)nextb);
	    }
	}
    }


    public static void textResponse(HttpExchange exchange,
    int status,
    String message)
    {
	try {
	    Headers responseHeaders = exchange.getResponseHeaders();
	    OutputStream outStream = exchange.getResponseBody();

	    responseHeaders.set("content-type", "text/plain");
	    exchange.sendResponseHeaders(200, message.length());
	    outStream.write(message.getBytes());

	    outStream.close();
	    exchange.close();
	} catch(IOException e) {
	    System.exit(1);
	}
    }
    /**
    functions to return a generic response, add codes as they are needed
    **/

    // 2xx
    public static void OK(HttpExchange exchange) { textResponse(exchange, 200, "200: OK"); }

    // 4xx
    public static void badRequest(HttpExchange exchange) { textResponse(exchange, 400, "400: Bad Request"); }
    public static void unauthorized(HttpExchange exchange) { textResponse(exchange, 401, "401: Unauthorized"); }
    public static void notFound(HttpExchange exchange) { textResponse(exchange, 404, "404: Not Found"); }
    public static void disallowedMethod(HttpExchange exchange) { textResponse(exchange, 405, "405: Method not Allowed"); }

    // 5xx
    public static void internalServerErr(HttpExchange exchange) { textResponse(exchange, 500, "500: Internal Server Error"); }

}
