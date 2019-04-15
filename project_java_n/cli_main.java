import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class cli_main
{
	public static void main(String args[])
	{
		Socket socket = null;
		SocketChannel nioSocket = null;
		
		InputStream is;
		DataInputStream dis;
		OutputStream os;
		DataOutputStream dos;
		
		ByteBuffer recvBuffer = null;
		ByteBuffer sendBuffer = null;
		
		int nSID = 5;
		int nCmd = 7;

		try
		{
			//socket = new Socket("127.0.0.1", 65010);
			nioSocket = SocketChannel.open();
			nioSocket.configureBlocking(true);
			nioSocket.connect(new InetSocketAddress("127.0.0.1", 65010));
			System.out.println("connect :" + nioSocket);

			//
			//cli_recvThread recvThread = new cli_recvThread(socket);
			cli_recv recvThread = new cli_recv(nioSocket);
			recvThread.start();
			
			//
			//cli_sendThread sendThread = new cli_sendThread(socket);
			cli_send sendThread = new cli_send(nioSocket);
			sendThread.start();	
			
			while( true )
			{
			}
		}
		catch( ConnectException conException )
		{
			System.err.println("connect fail.");
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}

		try 
		{
			if( null != nioSocket && nioSocket.isOpen() )
				nioSocket.close();
		}
		catch( IOException e )
		{
		}
	}
}