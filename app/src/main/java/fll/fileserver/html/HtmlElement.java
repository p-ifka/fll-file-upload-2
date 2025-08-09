package fll.fileserver.html;

import java.util.HashMap; // https://docs.oracle.com/javase/8/docs/api/java/util/HashMap.html
import java.util.Set;

public class HtmlElement
{
    private String name;
    private String contents;
    private HashMap<String, String> attributes;

    
    public HtmlElement(String elName)
    {
	name = elName;
	contents = "";
	attributes = new HashMap<String, String>();
    }

    
    /*getters*/
    public String  getName() { 	return name; }
    public String getContents() { return contents; }
    public HashMap<String, String> getAttributes() { return attributes; }
    public String getAttribute(String key) { return attributes.get(key); }
    
    
    /* attribute modification */
    
    public void setAttribute(String key,
			     String value)
    {
	if(attributes.containsKey(key)) {
	    attributes.replace(key, value);
	} else {
	    attributes.put(key, value);
	}
    }

    public void removeAttribute(String key)
    {
	attributes.remove(key);
    }

    public void clearAttributes()
    {
	attributes.clear();
    }


    /*contents modification */
    public void setContents(String content)
    {
	contents = content;
    }

    public void appendContents(String add)
    {
	contents = contents + add;
    }

    /* conversion to string */

    private String attributeString()
    {
	String attributeString = "";
	for(HashMap.Entry<String, String> entry : attributes.entrySet()) {
	    attributeString = attributeString + entry.getKey() + "=" + "\"" + entry.getValue() + "\" ";
	}
	return attributeString;
    }
    
    public String toElementString()
    {
	String elementString;
	elementString = "<" + name + attributeString() + ">" + contents + "</" + name + ">";
	return elementString;
    }

    public String toTag()
    {
	return "<" + name + attributeString() +  ">";
    }
    
}
