package fll.fileserver;

import fll.fileserver.Log;



import java.io.File;
import java.io.IOException;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Database
{

    private Log logInst;


    public Database(String dbPath, Log logInstance)
    {
	File dbFile;
	//	Connection dbCon;
	boolean needsInitialization = false;

	logInst = logInstance;

	dbFile = new File(dbPath);
	if(!dbFile.exists()) {
	    logInstance.warn("database file does not exist, initializing new table");
	    try {
		dbFile.createNewFile();
	    } catch(IOException e) {
		logInstance.error(String.format("failed to create file: %s: %s", dbPath, e.getMessage()));
	    }
	    needsInitialization = true;
	}

	try {
	    Connection dbCon = DriverManager.getConnection(String.format("jdbc:sqlite:%s", dbPath));
	    if(needsInitialization) {
		initializeHeadTable(dbCon);
	    }
	    close(dbCon);
	} catch(SQLException e) {
	    logInstance.fatal(String.format("failed to start connection to  database file: %s: %s", dbPath, e.getMessage()));
	    System.exit(1);
	}


    }

    public void close(Connection db) {
	try {
	    db.close();
	} catch(SQLException e) {
	    logInst.error(String.format("failed to close database: %s", e.getMessage()));
	}
    }


    private void initializeHeadTable(Connection db)
	throws SQLException
    {
	Statement stmt;

	logInst.info("initializing table");

	stmt = db.createStatement();

	stmt.execute("create table GROUPS"
		     + "(ID text primary key, EXPIRATION integer);");

	stmt.close();

	return;
    }


}
