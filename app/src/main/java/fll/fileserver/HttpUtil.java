package fll.fileserver;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpUtil
{
    private static final char[] HEX_CHAR_MAP = { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };


    public static String toHex(char value, boolean trimLeadingZeros)
    /**
    convert char to hex representation

    @param char value : value to convert
    @param boolean trimLeadingZeros : whether to trim extra zeros at beginning of output,
    when false, output length will be determined by type size.

    @return hexadecimal string
    **/
    {
	int byteSize;
	StringBuffer buf;
	int digitVal;
	boolean nonZero;

	byteSize = Character.SIZE / 8;
	buf = new StringBuffer(byteSize);
	nonZero = false;

	for(int i=0;i<byteSize*2;i++) {
	    digitVal = (( (value) >> (Character.SIZE - 4 - (i * 4))) & 0xf);
	    if(trimLeadingZeros && !nonZero && digitVal == 0) {
		continue;
	    }
	    nonZero = true;
	    buf.append(HEX_CHAR_MAP[digitVal]);
	}

	return buf.toString();
    }

    public static int parseHex(String hexstr)
    /**
    parse value from hexadecimal string

    @param String hexstr : string containing only valid hexadecimal digits (case insensitive) [a-z], [A-Z], [0-9]

    @return String value or -1 if hex string contains invalid characters
    **/
    {
	int result = 0;
	char ch;
	for(int i=0;i<hexstr.length();i++)
	{
	    ch = hexstr.charAt(i);
	    if(ch >= '0' && ch <= '9') {
		result = result + (((int)ch - (int)'0') * (int)(Math.pow(16, (hexstr.length() - i - 1))));
	    } else if(ch >= 'a' && ch <= 'f') {
		result = result + (10 + (int)ch - (int)'a') * (int)(Math.pow(16, hexstr.length() - i - 1));
	    } else if(ch >= 'A' && ch <= 'F') {
		result = result + (10 + (int)ch - (int)'A') * (int)(Math.pow(16, hexstr.length() - i - 1));
	    } else {
		return -1;
	    }
	}
	return result;
    }

    public static String escapeOutput(String str)
    /**
    escape characters for sending through http

    @param String str : string to escape

    @return escaped string

    TODO: only escape characters that have to be, instead of all of them
    **/
    {
	StringBuffer buf;
	int strlen;
	char ch;

	strlen = str.length();
	buf = new StringBuffer(strlen * 3);

	for(int i=0;i<strlen;i++) {
	    ch = str.charAt(i);
	    buf.append("%");
	    buf.append(toHex(ch, true));
	}
	return buf.toString();
    }

    public static String unescapeInput(String input)
    {
	StringBuffer buffer;
	char ch;

	buffer = new StringBuffer(input.length());
	for(int i=0;i<input.length();i++) {
	    ch = input.charAt(i);
	    if(ch == '%') {
		buffer.append((char)parseHex(String.format("%c%c", input.charAt(i + 1), input.charAt(i + 2))));
		i += 2;
	    } else {
		buffer.append((char)ch);
	    }
	}
	return buffer.toString();


    }


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
	int hexA, hexB;
	String hexstr;

	for(int i=0;true;i++) {
	    nextb = body.read();
	    if(nextb == -1) {
		if(i <= 0) {
		    return null;
		} else {
		    break;
		}
	    } else if((char)nextb == '&') {
		break;
	    } else {
		if(i >= bufferSize) {
		    return null;
		}
		parameterValue.append((char)nextb);
	    }
	}
	return unescapeInput(parameterValue.toString());
	
    }

    public static String multipartReadBoundary(InputStream body)
    throws IOException
    {
	int nb;
	StringBuffer buffer;

	buffer = new StringBuffer(60);

	for(int i=0;true;i++) {
	    nb = body.read();
	    if(nb == -1) {
		return null;
	    } else if((char)nb == '\r' || (char)nb == '\n') {
		return buffer.toString();
	    } else if(i >= 60){
		return null;
	    } else {
		buffer.append((char)nb);
	    }

	}
    }

    public static String multipartReadHeaders(InputStream body)
    throws IOException
    {
	int nb;
	int bufferSize;
	int leadNewlineChar;
	int newlineCount;
	StringBuffer buffer;

	newlineCount = 0;

	bufferSize = 1024;
	buffer = new StringBuffer(bufferSize);

	for(int i=0;true;i++) {
	    nb = body.read();
	    if(nb == -1) {
		return null;
	    } else if(nb == '\r' || nb == '\n') {
		newlineCount++;

		if(newlineCount >= 4) {
		    return buffer.toString();
		}

	    } else {
		newlineCount = 0;
		if(i >= bufferSize) {
		    bufferSize += 1024;
		    buffer.setLength(bufferSize);
		}
		buffer.append((char)nb);
	    }
	}

    }

    public static void multipartReadIntoFile(InputStream body,
    FileWriter targetFile,
    String stopLine)
    throws IOException
    {
	int bufferLength;
	int nb;
	StringBuffer line;
	String lineStr;

	bufferLength = 1024;
	line = new StringBuffer(bufferLength);

	for(int i=0;true;i++) {
	    nb = body.read();
	    if(nb == -1) {
		return;
	    } else {
		if(i >= bufferLength) {
		    bufferLength += 1024;
		    line.setLength(bufferLength);
		}
		// targetFile.write((char)nb);


		if((char)nb == '\n') {
		    lineStr = line.toString();

		    if(lineStr.length() >= stopLine.length() &&
		    lineStr.substring(0, stopLine.length()).equals(stopLine))
		    {
			return;
		    } else {
			targetFile.write(lineStr);
			targetFile.write((char)nb);
			line.delete(0, line.length());
		    }
		} else {
		    line.append((char)nb);
		}
	    }
	}

    }


    public static void textResponse(HttpExchange exchange,
    int status,
    String message)
    /**
    respond to http request in plain text with specified status and message

    @param HttpExchange exchange : request to respond to
    @param int status : response status
    @param String message : response message
    **/
    {
	try {
	    Headers responseHeaders = exchange.getResponseHeaders();
	    OutputStream outStream = exchange.getResponseBody();

	    responseHeaders.set("content-type", "text/plain");
	    exchange.sendResponseHeaders(status, message.length());
	    outStream.write(message.getBytes());

	    outStream.close();
	    exchange.close();
	} catch(IOException e) {

	}
    }

    public static void redirect(HttpExchange exchange,
    String url)
    /**
    respond to Http request with redirect (302) to specified URL

    @param HttpExchange exchange : exchange of request to respond to
    @param String url : location header of response
    **/
    {
	try {
	    Headers responseHeaders = exchange.getResponseHeaders();
	    OutputStream responseBody = exchange.getResponseBody();

	    responseHeaders.set("location", url);
	    exchange.sendResponseHeaders(302, 0);

	    responseBody.close();
	    exchange.close();
	} catch(IOException e) {

	}
    }

    
    public static void redirectSeeOther(HttpExchange exchange,
    String url)
    /**
    respond to Http request with redirect (303: see other) to specified URL

    @param HttpExchange exchange : exchange of request to respond to
    @param String url : location header of response
    **/
    {
	try {
	    Headers responseHeaders = exchange.getResponseHeaders();
	    OutputStream responseBody = exchange.getResponseBody();

	    responseHeaders.set("location", url);
	    exchange.sendResponseHeaders(303, 0);

	    responseBody.close();
	    exchange.close();
	} catch(IOException e) {

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
