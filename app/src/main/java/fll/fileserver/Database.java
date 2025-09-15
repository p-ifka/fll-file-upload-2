package fll.fileserver;

import fll.fileserver.Log;
import fll.fileserver.FileManager;

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


    private Log log;
    private FileManager fileMgr;
    private Connection database;

    private final String GROUP_LIST_TABLE = "GROUPS";
    private final String USER_LIST_TABLE = "USERS";

    public class SqlQuery {
	public Statement statement;
	public ResultSet result;

	public void free()
	throws SQLException
	{
	    statement.close();
	    result.close();
	}
    }




    public Database(String dbPath, Log log, FileManager fileMgr)
    {
	File dbFile;
	//	Connection dbCon;
	boolean needsInitialization = false;

	this.log = log;
	this.fileMgr = fileMgr;

	dbFile = new File(dbPath);
	if(!dbFile.exists()) {
	    log.warn("database file does not exist, initializing new table");
	    try {
		dbFile.createNewFile();
	    } catch(IOException e) {
		log.error(String.format("failed to create file: %s: %s", dbPath, e.getMessage()));
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
	    log.fatal(String.format("failed to start connection to  database file: %s: %s", dbPath, e.getMessage()));
	    System.exit(1);
	}


    }


    private void initializeGroupTable(Connection db)
    throws SQLException
    {
	Statement stmt;

	log.info(String.format("initializing table:  %s", GROUP_LIST_TABLE));

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

	log.info(String.format("initializing table:  %s", USER_LIST_TABLE));

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
	    log.error(String.format("failed to close database: %s", e.getMessage()));
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

    private String escapeInput(String input)
    {
	return input.replace("\"", "\"\"").replace("\'", "\'\'");
    }

    private boolean isValidGroupName(String groupID)
    {
	for(int i=0;i<groupID.length();i++) {
	    char ch = groupID.charAt(i);
	    if(!( (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || (ch == '_') || (ch == '-'))) {
		return false;
	    }
	}

	return true;

    }

    public boolean doesGroupExist(String groupID)
    throws SQLException
    {
	Statement stmt;
	ResultSet result;

	stmt = database.createStatement();
	result = stmt.executeQuery(String.format("select * from %s where GROUPID = \"%s\"", USER_LIST_TABLE, escapeInput(groupID)));

	if(result.next()) {
	    return true;
	} else {
	    return false;
	}

    }

    public int authenticateAny(String pass)
    {
	Statement stmt;
	ResultSet result;
	try {
	    stmt = database.createStatement();
	    result = stmt.executeQuery(String.format("select * from %s where pass = \"%s\"", USER_LIST_TABLE, escapeInput(pass)));
	    
	    if(result.next()) {
		return DB_SUCCESS;
	    } else {
		return DB_ISSUE;
	    }
	} catch(SQLException e) {
	    log.error(String.format("|Database.authenticateAny| SQLException : %s", e.getMessage()));
	    return DB_ERROR;
	}
    }

    
    
    public SqlQuery getGroups()
    throws SQLException
    /**
    get list and data for all groups

    @return SqlQuery : result of query
    **/
    {

	Statement stmt;
	String sql;
	ResultSet result;
	SqlQuery ret;

	stmt = database.createStatement();
	sql = String.format("select * from %s", GROUP_LIST_TABLE);

	log.info(String.format("executing sql: %s", sql));

	result =  stmt.executeQuery(sql);

	ret = new SqlQuery();
	ret.statement = stmt;
	ret.result = result;

	return ret;
    }

    public SqlQuery getUsersInGroup(String groupID)
    throws SQLException
    /**
    get list of users that are in group

    @return SqlQuery : result of query
    **/
    {
	Statement stmt;
	ResultSet result;
	SqlQuery ret;

	stmt = database.createStatement();
	result = stmt.executeQuery(String.format("select * from %s where GROUPID = \"%s\"", USER_LIST_TABLE, escapeInput(groupID)));

	ret = new SqlQuery();
	ret.statement = stmt;
	ret.result = result;

	return ret;
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
	    result =  stmt.executeQuery(String.format("select count(GROUPID) from %s where GROUPID = \"%s\"", USER_LIST_TABLE, escapeInput(group)));

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
	    log.error(String.format("|Database.getUserCount| SQLException : %s", e.getMessage()));
	    return DB_ERROR;
	}

    }

    public int createGroup(String groupID,
    String expiration)
    /**
    attempt to initialize new group
    @param String groupId : name of new group
    @param String expiration : date of expiration in YYYY-MM-DD format, or DB_EXPIRY_NONE to disable expiry

    @return DB_ERROR on error, DB_ISSUE if group already exists, DB_SUCCESS if group was successfully created
    **/
    {
	if(!isValidGroupName(groupID)) {
	    return DB_ISSUE;
	}
	
	log.info(String.format("attempting to create group %s", groupID));

	Statement stmt;
	String sql;

	try {
	    stmt = database.createStatement();

	    /* check if group is already in table */
	    sql = String.format("select ID from %s where ID = \"%s\"", GROUP_LIST_TABLE, groupID);
	    log.info(String.format("SQL: %s", sql));
	    ResultSet rs = stmt.executeQuery(sql);

	    if(rs.next()) {
		log.warn(String.format("group %s already exists", groupID));
		return DB_ISSUE;
	    }

	    /* create group */
	    sql = String.format("insert into %s (id, expiration) values(\"%s\", \"%s\")", GROUP_LIST_TABLE, groupID, expiration);
	    log.info(String.format("executing sql: %s", sql));
	    int rc = stmt.executeUpdate(sql);

	    log.info(String.format("SQL: %s return %d", sql, rc));

	    rs.close();
	    stmt.close();

	    return DB_SUCCESS;

	} catch(SQLException e) {
	    log.error(String.format("SQLException: %s", e.getMessage()));
	    return DB_ERROR;
	}

    }

    public int createUsers(String groupID, int quantity)
    {
	log.info(String.format("creating %d users in group %s", quantity, groupID));

	Statement stmt;
	String pass;
	ResultSet rs;
	int fileRC;
	try {
	    /* make sure group exists */
	    stmt = database.createStatement();
	    rs = stmt.executeQuery( String.format("select * from %s where ID = \"%s\"", GROUP_LIST_TABLE, groupID));
	    if(!rs.next()) {
		log.warn(String.format("request to create user(s) failed: group %s does not exist" , groupID));
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
		+ "values (\"%s\", \"%s\")", USER_LIST_TABLE, pass, groupID));

		fileRC = fileMgr.createUserDir(pass);
		if(fileRC == FileManager.FS_ERROR) {
		    return DB_ERROR;
		}
		
	    }

	    return DB_SUCCESS;

	} catch(SQLException e) {
	    log.error("|Database.createUsers| SQLException: %s" + e.getMessage());
	    return DB_ERROR;
	}



    }

}
 
