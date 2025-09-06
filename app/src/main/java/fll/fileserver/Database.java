package fll.fileserver;

import fll.fileserver.Log;

import java.util.Random;

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
    private final String USER_LIST_TABLE = "USERS";

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
		initializeGroupTable(dbCon);
		initializeUserTable(dbCon);
	    }
	    database = dbCon;

	} catch(SQLException e) {
	    logInstance.fatal(String.format("failed to start connection to  database file: %s: %s", dbPath, e.getMessage()));
	    System.exit(1);
	}


    }


    private void initializeGroupTable(Connection db)
    throws SQLException
    {
	Statement stmt;

	logInst.info(String.format("initializing table:  %s", GROUP_LIST_TABLE));

	stmt = db.createStatement();

	stmt.execute(String.format("create table %s"
	+ "(ID text primary key, EXPIRATION text);", GROUP_LIST_TABLE));

	stmt.close();

	return;
    }

    private void initializeUserTable(Connection db)
    throws SQLException
    {
	Statement stmt;

	logInst.info(String.format("initializing table:  %s", USER_LIST_TABLE));

	stmt = db.createStatement();
	stmt.execute(String.format("create table %s"
	+ "(PASS text primary key, GROUPID text, foreign key(GROUPID) references %s(ID))", USER_LIST_TABLE, GROUP_LIST_TABLE));

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


    private String randomPassword()
    /**
    generate random sequence of 4 numbers

    @return string containing 4 random numbers
    **/
    {
	StringBuffer passNum;
	StringBuffer pass;
	Random rand;

	passNum = new StringBuffer(4);
	pass = new StringBuffer(4);
	rand = new Random();

	passNum.insert(0, rand.nextInt(9999));

	for(int i=0;i<4-passNum.length();i++) {
	    pass.insert(i, "0");
	}
	pass.append(passNum);

	return pass.toString();
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

    public int getUserCount(String group)
    /**
    get the number of users in a group

    @param String group : ID of group to get user count for

    @return number of users in group, or DB_ERROR if there is an error
    **/
    {
	Statement stmt;
	ResultSet result;
	int count;
	try {
	    stmt = database.createStatement();
	    result =  stmt.executeQuery(String.format("select count(GROUPID) from %s where GROUPID = \"%s\"", USER_LIST_TABLE, group));

	    if(result.next()) {
		count = result.getInt(1);
		stmt.close();
		result.close();
		return count;
	    } else {
		stmt.close();
		result.close();
		return -1;
	    }
	    
	} catch(SQLException e) {
	    logInst.error(String.format("|Database.getUserCount| SQLException : %s", e.getMessage()));
	    return DB_ERROR;
	}
	
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
	    sql = String.format("insert into %s (id, expiration) values(\"%s\", \"%s\")", GROUP_LIST_TABLE, groupId, expiration);
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

    public int createUsers(String group, int quantity)
    {
	logInst.info(String.format("creating %d users in group %s", quantity, group));
	
	Statement stmt;
	String pass;
	ResultSet rs;
	try {
	    /* make sure group exists */
	    stmt = database.createStatement();
	    rs = stmt.executeQuery( String.format("select * from %s where ID = \"%s\"", GROUP_LIST_TABLE, group));
	    if(!rs.next()) {
		logInst.warn(String.format("request to create user(s) failed: group %s does not exist" , group));
		return DB_ISSUE;
	    }

	    for(int i=0;i<quantity;i++) {

		while(true) {
		    pass = randomPassword();
		    rs = stmt.executeQuery(String.format("select * from %s where PASS = \"%s\"", USER_LIST_TABLE, pass));
		    if(!rs.next()) {
			break;
		    }
		}

		stmt.execute(String.format("insert into %s (PASS, GROUPID)"
		+ "values (\"%s\", \"%s\")", USER_LIST_TABLE, pass, group));
	    }

	    return DB_SUCCESS;

	} catch(SQLException e) {
	    logInst.error("|Database.createUsers| SQLException: %s" + e.getMessage());
	    return DB_ERROR;
	}



    }

}
