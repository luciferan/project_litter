import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import java.util.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

//
class svr_sendThread implements Runnable
{
	svr_session session = null;
	
	//
	svr_sendThread(svr_session session)
	{
		setSession(session);
	}

	void setSession(svr_session session)
	{
		this.session = session;
	}

	@Override
	public void run()
	{
		System.out.println("# svr_sendThread: run() start.");
		
		try
		{
			session.procSend();
		}
		catch( Exception e )
		{
		}
		finally
		{
		}
	}
}

//
class svr_recvThread implements Runnable
{
	svr_session session = null;
	
	//
	svr_recvThread(svr_session session)
	{
		setSession(session);
	}

	void setSession(svr_session session)
	{
		this.session = session;
	}
	
	@Override
	public void run()
	{
		System.out.println("# svr_recvThread: run() start.");
		
		try
		{
			session.procRecv();
		}
		catch( Exception e )
		{
		}
		finally
		{
		}
	}
}