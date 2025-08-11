package fll.fileserver;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

public class Log
{
    private boolean loggingEnabled;
    private String logPath;
    private FileWriter logWriter;


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

	    logWriter = new FileWriter(logFile, true);
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
		System.out.println("[ERROR]   failed to write to log file: " + e.getMessage());
	    }
	}
    }

    public void close()
    {
	try {
	    info("closing log stream");
	    logWriter.close();
	} catch(IOException e) {
	    System.out.println("failed to close log filewriter: " + e.getMessage());
	}
    }

    public void info(String msg) { doLog(String.format("[INFO]   %s\n", msg)); }
    public void warn(String msg) { doLog(String.format("[WARN]   %s\n", msg)); }
    public void error(String msg) { doLog(String.format("[ERROR]   %s\n", msg)); }
}
