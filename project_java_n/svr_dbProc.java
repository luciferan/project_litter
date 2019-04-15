import java.sql.*;

class svr_dbProc
{
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost:3306/test";
	static final String USERNAME = "testAdmin";
	static final String PASSWORD = "testAdmin";
	
	static Connection conn = null;
	static Statement stmt = null;
	
	public svr_dbProc()
	{
		try
		{
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
			System.out.println("MySql connection.");
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
		}
	}
	
	static void procEnd()
	{
		try 
		{
			if( null != stmt )
				stmt.close();
		}
		catch( SQLException e_sql ) {}
		try
		{
			if( null != conn )
				conn.close();
		}
		catch( SQLException e_sql ) {}
	}
		
	static public void writeLog(int nType, String strContext)
	{
		//System.out.println("writeLog start.");
		try 
		{
			stmt = conn.createStatement();
			
			String sql;
			sql = "insert into test_table values (now()," + nType + ",'" + strContext + "')";
			int r = stmt.executeUpdate(sql);
			//System.out.println("executeUpdate() result " + r);
		}
		catch( SQLException e )
		{
			e.printStackTrace();
		}
		finally
		{
		}
	}

	public static void main(String args[])
	{
		System.out.println("svr_dbProc() start.");
		try
		{
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
			System.out.println("MySql connection.");

			stmt = conn.createStatement();
			
			String sql;
			sql = "select date, type, context from test_table";
			ResultSet rs = stmt.executeQuery(sql);
			
			while( rs.next() )
			{
				java.sql.Date date = rs.getDate("date");
				int type = rs.getInt("type");
				String context = rs.getString("context");
				
				System.out.println(date.toString() + "\t" + type + "\t" + context);
			}
			
			rs.close();
		}
		catch( SQLException e_sql )
		{
			e_sql.printStackTrace();
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			procEnd();
		}
		
		System.out.println("svr_dbProc end.");
	}
}