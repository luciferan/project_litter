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
class svr_workerThread implements Runnable //extends Thread
{
	svr_dataManager dataManager = null;
	HashMap<Long, svr_session> mapSessionList = null;
	HashMap<Long, Thread> mapSessionProcList = null;
	
	Selector selector = null;
	ServerSocketChannel nioListenSocket = null;

	boolean bSvrStart = false;
	
	//
	svr_workerThread(svr_dataManager dataManager)
	{
		this.dataManager = dataManager;
		this.mapSessionList = dataManager.getSessionList();
		this.mapSessionProcList = new HashMap<>();
	}
	
	//
	void svrStart()
	{
		try
		{
			selector = Selector.open();
			
			nioListenSocket = ServerSocketChannel.open();
			nioListenSocket.configureBlocking(false);
			nioListenSocket.bind(new InetSocketAddress(65010));
			nioListenSocket.register(selector, SelectionKey.OP_ACCEPT);
			
			bSvrStart = true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	void svrStop()
	{
	}
	
	void onAccept(SelectionKey key)
	{
		try
		{
			ServerSocketChannel listenSocket = (ServerSocketChannel)key.channel();
			SocketChannel acceptSocket = listenSocket.accept();
			acceptSocket.setOption(StandardSocketOptions.TCP_NODELAY, true);
			
			System.out.println("accept.");
			
			svr_session session = new svr_session(dataManager.makeSessionKey(), acceptSocket, selector, dataManager);
			//Thread sessionThread = new Thread(session);
			mapSessionList.put(session.getIndex(), session);
			//mapSessionProcList.put(session.getIndex(), sessionThread);
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	void onRecv()
	{
	}

	@Override
	public void run()
	{
		System.out.println("# svr_workerThread: run() start.");
		
		try
		{
			if( false == bSvrStart )
				svrStart();
			
			ExecutorService execService = Executors.newFixedThreadPool(4);
			
			//
			while( false == Thread.currentThread().isInterrupted() )
			{
				int keyCount = selector.select();
				if( 0 == keyCount )
					continue;
				
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iter = selectedKeys.iterator();
				
				while( iter.hasNext() )
				{
					SelectionKey key = iter.next();
					svr_session session = (svr_session)key.attachment();
					
					if( key.isAcceptable() )
					{
						//System.out.println("# svr_workerThread: accept.");
						onAccept(key);
					}
					else if( key.isReadable() )
					{
						//System.out.println("# svr_workerThread: recv.");
						
						int nRet = session.onRecv();
						if( -1 == nRet )
						{
							session.disconnect();
							System.out.println("## svr_workerThread: recv -1. [" + session.getIndex() + "] disconnect.");
							mapSessionList.remove(session.getIndex());
							mapSessionProcList.remove(session.getIndex());
							System.out.println("## svr_workerThread: left connection " + mapSessionList.size());
						}
						else
						{
							session.procRecv();

							// Thread t = new Thread(new svr_recvThread(session));
							// if( null != t )
								// execService.submit(t);
							
							// execService.submit(session.getRecvThread());
						}
					}
					else if( key.isWritable() )
					{
						//System.out.println("# svr_workerThread: send.");
						session.writeLog("# svr_workerThread: send.");

						int nRet = session.onSend();
						if( -1 == nRet )
						{
							session.disconnect();
							System.out.println("## svr_workerThread: send -1. [" + session.getIndex() + "] disconnect.");
							mapSessionList.remove(session.getIndex());
							mapSessionProcList.remove(session.getIndex());
							System.out.println("## svr_workerThread: left connection " + mapSessionList.size());
						}
						else if( 0 < nRet )
						{
							// session.procSend();
							
							// Thread t = new Thread(new svr_sendThread(session));
							// if( null != t )
								// execService.submit(t);
							
							execService.submit(session.getSendThread());
						}
					}
					
					iter.remove();
				}
			}
		}
		catch( IOException e )
		{
		}
		finally
		{
			if( null != nioListenSocket && nioListenSocket.isOpen() )
				svrStop();
		}
		
		System.out.println("svr_workerThread: run() end.");
	}
}