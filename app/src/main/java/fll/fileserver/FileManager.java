package fll.fileserver;

import fll.fileserver.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;

import java.io.IOException;

import java.lang.SecurityException;


public class FileManager
{
    public static final int FS_SUCCESS = 0;
    public static final int FS_ISSUE = 1;
    public static final int FS_ERROR = -1;

    private class FilterNonHidden
    implements FilenameFilter
    {
	@Override
	public boolean accept(File dir, String name)
	{
	    if(name.charAt(0) != '.') {
		return true;
	    } else {
		return false;
	    }
	}
    }

    private Log log;
    private String filesRoot;

    public FileManager(Log logInstance, String rootDir)
    {
	log = logInstance;
	filesRoot = rootDir;
    }

    public int createUserDir(String userID)
    {
	File gDir;

	gDir = new File(String.format("%s/%s/", filesRoot, userID));
	log.info(String.format("creating directory  %s/%s/", filesRoot, userID));
	if(gDir.exists()) {
	    return FS_SUCCESS;
	} else {
	    try {
		gDir.mkdir();
		return FS_SUCCESS;
	    } catch(SecurityException e) {
		log.error(String.format("|Files.createUserDir| SecurityException : %s", e.getMessage()));
		return FS_ERROR;
	    }
	}
    }

    public boolean fileExists(String userID,
    String path)
    {
	File file;

	file = new File(String.format("%s/%s/%s", filesRoot, userID, path));
	return file.exists();
    }

    public FileWriter openNewFileWrite(String userID,
    String path)
    throws IOException
    {
	File newFile;


	newFile = new File(String.format("%s/%s/%s", filesRoot, userID, path));
	log.info(String.format("creating file %s/%S/%s", filesRoot, userID, path));
	newFile.createNewFile();
	newFile.setWritable(true, true);
	return new FileWriter(newFile);
    }

    
    public File openFile(String userID,
    String path)
    throws FileNotFoundException
    {
	File file;
	file = new File(String.format("%s/%s/%s", filesRoot, userID, path));
	return file;
    }

    
    public FileReader openFileRead(String userID,
    String path)
    throws FileNotFoundException
    {
	    File readFile;
	    readFile = new File(String.format("%s/%s/%s", filesRoot, userID, path));
	    return new FileReader(readFile);
    }



    public File[] listFiles(String userID, String relativePath)
    {
	try {
	    return new File(String.format("%s/%s/%s", filesRoot, userID, relativePath)).listFiles(new FilterNonHidden());
	} catch(SecurityException e) {
	    log.error(String.format("|FileManager.listFiles| SecurityException : %s", e.getMessage()));
	    return null;
	}
    }




}
