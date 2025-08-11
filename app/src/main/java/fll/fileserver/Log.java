package fll.fileserver;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Date;

public class Log
{
    private boolean loggingEnabled;
    private String logPath;
    private BufferedWriter logWriter;


    public Log(String path)
    {
	logPath = path;
	try {
	    // if(!File.exists(path)) {
		// File.createNewFile(path);
	    // }

	    File logFile = new File(path);
	    logFile.createNewFile();
	    if(!logFile.canWrite()) {
		System.out.println(String.format("log file %s cannot be written to", path));
		loggingEnabled = false;
		return;
	    }
	    
	    
	    logWriter = new BufferedWriter(new FileWriter(logPath, true));
	    loggingEnabled = true;
	    info("opened log file" + path);
	} catch(IOException e) {
	    System.out.println("failed to open log file:" + logPath + ": " + e.getMessage());
	    loggingEnabled = false;
	}

    }

    public static String defaultLogPath()
    {
	Date logDate = new Date();
	return String.format("logs/default.log");

    }


    private void doLog(String logMsg) {
	if(loggingEnabled) {
	    System.out.println(logMsg);
	    try {
		logWriter.append(logMsg);
	    } catch(IOException e) {
		System.out.println("[ERROR]   FAILED TO WRITE TO LOG");
	    }
	}
    }


    public void info(String msg) { doLog(String.format("[INFO]   %s", msg)); }
    public void warn(String msg) { doLog(String.format("[WARN]   %s", msg)); }
    public void error(String msg) { doLog(String.format("[ERROR]   %s", msg)); }
}
