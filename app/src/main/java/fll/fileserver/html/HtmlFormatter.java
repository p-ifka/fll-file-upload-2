package fll.fileserver.html;

import java.util.HashMap;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

public class HtmlFormatter
{
    private static final char FORMAT_INDICATOR = '~';

    public static String readWholePageFile(String path)
    /**
    return the contents of file as String

    @param String path : path to file
    **/
    {
	int bufferSize = 1024;
	StringBuffer buffer = new StringBuffer(bufferSize);
	File page;
	InputStream pageReader;
	int nextb;


	page = new File(path);
	if(page.exists() && page.canRead()) {
	    try {
		pageReader = new FileInputStream(page);
		for(int i=0;true;i++) {
		    nextb = pageReader.read();
		    if(nextb == - 1) {
			System.out.println("page reader completed after " + i + "bytes");
			return buffer.toString();
		    } else {
			if(i >= bufferSize) {
			    bufferSize += 1024;
			    buffer.setLength(bufferSize);
			}
			buffer.append((char)nextb);
		    }
		}
	    } catch(IOException e) {
		System.out.println(String.format("|HtmlFormatter.readWholePageFile| IOException : %s", e.getMessage()));
		return "";
	    }
	}

	return "";
    }

    
    public static String format(String fmt,
				HashMap<String, String> args)
    /** format string using named tags surrounded by **/
    {
	String formattedStr = "";

	for(int i=0; i<fmt.length(); i++) {
	    if(fmt.charAt(i) == FORMAT_INDICATOR) {
		if(i + 1 > fmt.length()) { return formattedStr + fmt.charAt(i); } // TODO: log warning if this ever happens, it should not
		
		if(fmt.charAt(i + 1) == FORMAT_INDICATOR) {
		    formattedStr = formattedStr + FORMAT_INDICATOR;
		    i++;
		    continue;
		}
		
		
		
		

		for(int j=i + 1; j<fmt.length(); j++) {
		    if(fmt.charAt(j) == FORMAT_INDICATOR) {
			formattedStr = formattedStr + args.get(fmt.substring(i + 1, j));
			i = j;
			break;
		    }
		}

	    } else {
		formattedStr = formattedStr + fmt.charAt(i);
	    }
	}

	return formattedStr;
    }

    
    
}
