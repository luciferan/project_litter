import java.io.*;
import java.sql.*;

//
class log
{
	static File file = null;
	
	static void setFileName(String fileName)
	{
		file = new File(fileName);
	}
}

//
class svr_log extends log
{
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost:3306/test";
	static final String USERNAME = "testAdmin";
	static final String PASSWORD = "testAdmin";
	
	static Connection conn = null;
	static Statement stmt = null;
	
	//
	svr_log(String fileName)
	{
		setFileName(fileName);
		// setDBConn();
	}
	
	void finalizer()
	{
		try 
		{
			if( null != stmt )
				stmt.close();
		}
		catch( SQLException e_sql ) 
		{}
		try
		{
			if( null != conn )
				conn.close();
		}
		catch( SQLException e_sql ) 
		{}
	}		

	//
	static int setDBConn()
	{
		try
		{
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
			// System.out.println("MySql connection.");
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
		return 0;
	}
	
	//
	static void writeLog(String str)
	{
		writeFileLog("# svr_log.writeLog(): " + str);
		System.out.println(str);
		
		writeFileLog(str);
		writeDBLog(0, str);
	}
	
	static private void writeFileLog(String str)
	{
		try
		{
			FileWriter fw = new FileWriter(file, true);
			fw.write(str + "\r\n");
			fw.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}		
	}
	
	static private void writeDBLog(int type, String str)
	{
		writeFileLog("# writeDBLog(): " + str);
		
		try 
		{
			stmt = conn.createStatement();
			
			String sql;
			sql = "insert into test_table values (now()," + type + ",'" + str + "')";
			int r = stmt.executeUpdate(sql);
			writeFileLog("## executeUpdate() result " + r);
		}
		catch( SQLException e )
		{
			e.printStackTrace();
		}		
	}
	
	//
	public static void main(String args[])
	{
		setFileName("test_log.log");
		setDBConn();
		
		//
		writeLog("exrfrt");
	}
}