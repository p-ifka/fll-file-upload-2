package fll.fileserver;

import fll.fileserver.Log;



import java.io.File;
import java.io.IOException;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;


public class Database
{
    /* return values used by database functions */
    public static final int DB_SUCCESS = 1; // for when a function completes sucessfully
    public static final int DB_ISSUE = 0;   // for when a function fails because of intentional mechanism (ex. user input error)
    public static final int DB_ERROR = -1;  // for when an unexpected error/exception occurs

    public static final String DB_EXPIRY_NONE = "none";


    private Log logInst;
    private Connection database;

    private final String GROUP_LIST_TABLE = "GROUPS";

    public class SqlQuery {
	public Statement stmt;
	public ResultSet result;

	public void free()
	throws SQLException
	{
	    stmt.close();
	    result.close();
	}
    }




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
	    database = dbCon;
	    // createGroup("group3", -1);

	    // close(dbCon);
	} catch(SQLException e) {
	    logInstance.fatal(String.format("failed to start connection to  database file: %s: %s", dbPath, e.getMessage()));
	    System.exit(1);
	}


    }


    private void initializeHeadTable(Connection db)
    throws SQLException
    {
	Statement stmt;

	logInst.info("initializing table");

	stmt = db.createStatement();

	stmt.execute("create table GROUPS"
	+ "(ID text primary key, EXPIRATION text);");

	stmt.close();

	return;
    }

    public void close() {
	try {
	    database.close();
	} catch(SQLException e) {
	    logInst.error(String.format("failed to close database: %s", e.getMessage()));
	}
    }

    public SqlQuery getGroups()
    throws SQLException
    /**
    get list and data for all groups
    @return ResultSet for all groups in table groups
    **/
    {

	Statement stmt;
	String sql;
	ResultSet result;
	SqlQuery retst;

	stmt = database.createStatement();
	sql = String.format("select * from %s", GROUP_LIST_TABLE);

	logInst.info(String.format("executing sql: %s", sql));

	result =  stmt.executeQuery(sql);
	
	retst = new SqlQuery();
	retst.stmt = stmt;
	retst.result = result;

	return retst;
    }

    public int createGroup(String groupId,
    String expiration)
    /**
    attempt to initialize new group
    @param String groupId : name of new group
    @param String expiration : date of expiration in YYYY-MM-DD format, or DB_EXPIRY_NONE to disable expiry

    @return DB_ERROR on error, DB_ISSUE if group already exists, DB_SUCCESS if group was successfully created
    **/
    {
	logInst.info(String.format("attempting to create group %s", groupId));

	Statement stmt;
	String sql;

	try {
	    stmt = database.createStatement();

	    /* check if group is already in table */
	    sql = String.format("select ID from %s where ID = \"%s\"", GROUP_LIST_TABLE, groupId);
	    logInst.info(String.format("SQL: %s", sql));
	    ResultSet rs = stmt.executeQuery(sql);

	    if(rs.next()) {
		logInst.warn(String.format("group %s already exists", groupId));
		return DB_ISSUE;
	    }

	    /* create group */
	    sql = String.format("insert into GROUPS (id, expiration) values(\"%s\", \"%s\")", groupId, expiration);
	    logInst.info(String.format("executing sql: %s", sql));
	    int rc = stmt.executeUpdate(sql);

	    logInst.info(String.format("SQL: %s return %d", sql, rc));

	    rs.close();
	    stmt.close();

	    return DB_SUCCESS;

	} catch(SQLException e) {
	    logInst.error(String.format("SQLException: %s", e.getMessage()));
	    return DB_ERROR;
	}

    }

}
