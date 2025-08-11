package fll.fileserver.html;

import java.util.HashMap;

public class HtmlFormatter
{
    private static final char FORMAT_INDICATOR = '~';


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
