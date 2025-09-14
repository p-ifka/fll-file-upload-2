package fll.fileserver;

import fll.fileserver.Log;

import java.io.File;
import java.io.IOException;




public class FileManager
{
    public static final int FS_SUCCESS = 0;
    public static final int FS_ISSUE = 1;
    public static final int FS_ERROR = -1;

    
    
    private Log log;
    private String filesRoot;
    
    public FileManager(Log logInstance, String rootDir)
    {
	log = logInstance;
	filesRoot = rootDir;
    }
    
    public int createGroupDir(String groupID)
    {
	File gDir;

	gdir = new File(String.format("%s/%s/", filesRoot, groupID));
	
	if(gDir.exists()) {
	    gDir.close();
	    return FS_SUCCESS;
	} else {
	    try {
		gDir.mkdir();
		gDir.close();
		return FS_SUCCESS;
	    } catch(IOException e) {
		gDir.close();
		log.error(String.format("|Files.createGroupDir| IOEXception : %s", e.getMessage()));
		return FS_ERROR;
	    }
	}
    }
    

    
    
}
