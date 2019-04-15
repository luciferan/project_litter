import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.*;
import java.net.*;
import java.lang.Thread;
import java.util.*;

//
public class cli_send extends Thread
{
	SocketChannel nioSocket = null;

	//
	cli_send(SocketChannel nioSocket)
	{
		this.nioSocket = nioSocket;
	}
	
	public void run()
	{
		int offset = 0, tail = 0, remain = 0;
		ByteBuffer sendBuffer = ByteBuffer.allocate(1024*10);
		ByteBuffer sendPacket = ByteBuffer.allocate(1024);
		
		Scanner sc = null;
		Charset cs = Charset.forName("UTF-8");
		
		//
		try
		{
			sc = new Scanner(System.in);
			String cmd;
			
			System.out.println("cli_sendThread start().");
			//System.out.println("socket: " + nioSocket);

			//
			{
				System.out.print("regist id: ");
				cmd = sc.nextLine();
				
				reqConnection packet = new reqConnection();
				packet.setName(cmd);
				packet.makePacket();
				
				sendBuffer.clear();
				sendBuffer.put(packet.getData().array(), 0, packet.getDataSize());
				sendBuffer.flip();
				//System.out.println("# send 1: packet: " + packet);
				//System.out.println("# send 1: buffer: " + sendBuffer);
				nioSocket.write(sendBuffer);
			}
			
			sc.reset();

			int protocol = 0x0002;
			int repeat = 0;

			while( true )
			{
				cmd = sc.nextLine();
				
				if( cmd.equals("send all") )
				{
					protocol = 0x0002;
				}
				else if( cmd.equals("send any") )
				{
					protocol = 0x0003;
				}
				else if( cmd.equals("repeat") )
				{
					repeat = 10;
				}
				else
				{
					for( ; 0 <= repeat ; --repeat )
					{
						if( 0 < repeat )
							Thread.sleep(1500);
						
						switch( protocol )
						{
						case 0x0002:
							{
								reqMsg_All packet = new reqMsg_All();
								packet.setMsg(cmd);
								packet.makePacket();

								//
								sendBuffer.clear();
								sendBuffer.put(packet.getData().array(), 0, packet.getDataSize());
								sendBuffer.flip();
								//System.out.println("# send 2: packet: " + packet);
								//System.out.println("# send 2: buffer: " + sendBuffer);
								nioSocket.write(sendBuffer);						
							}
							break;
						case 0x0003:
							{
								reqMsg_Any packet = new reqMsg_Any();
								packet.setMsg(cmd);
								packet.makePacket();
								
								//
								sendBuffer.clear();
								sendBuffer.put(packet.getData().array(), 0, packet.getDataSize());
								sendBuffer.flip();
								//System.out.println("# send 2: packet: " + packet);
								//System.out.println("# send 2: buffer: " + sendBuffer);
								nioSocket.write(sendBuffer);
							}
							break;
						}
					}
				}
			}
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
		catch( InterruptedException e )
		{
		}
		finally
		{
			System.out.println("cli_sendThread end.()");
		}
	}
	
	public static void main(String args[])
	{
		SocketChannel nioSocket = null;
		
		try
		{
			ByteBuffer sendBuffer = ByteBuffer.allocate(1024*10);
			ByteBuffer sendPacket = ByteBuffer.allocate(1024);
			
			nioSocket = SocketChannel.open();
			nioSocket.configureBlocking(true);
			nioSocket.setOption(StandardSocketOptions.TCP_NODELAY, true);
			
			nioSocket.connect(new InetSocketAddress("127.0.0.1", 65010));
			System.out.println("connect: " + nioSocket);
			
			Charset cs = Charset.forName("UTF-8");

			//if( false )
			{
				sendPacket.clear();
				String str = "hello";
				sendPacket.putInt(0x0001);
				sendPacket.put(cs.encode(str));
				int nSendSize = sendPacket.position();
				sendPacket.flip();

				//
				sendBuffer.clear();
				sendBuffer.putInt(nSendSize);
				sendBuffer.put(sendPacket.array(), 0, nSendSize);
				sendBuffer.flip();
				
				//System.out.println("sendPacket: " + sendPacket);
				//System.out.println("sendBuffer: " + sendBuffer);

				System.out.println("send 1: " + sendBuffer);
				nioSocket.write(sendBuffer);
			}
			
			//if( false )
			{
				sendPacket.clear();
				String str = "world";
				sendPacket.putInt(0x0002);
				sendPacket.put(cs.encode(str));
				int nSendSize = sendPacket.position();
				sendPacket.flip();
				
				//
				sendBuffer.clear();
				sendBuffer.putInt(nSendSize);
				sendBuffer.put(sendPacket.array(), 0, nSendSize);
				sendBuffer.flip();
				
				System.out.println("send 2: " + sendBuffer);
				nioSocket.write(sendBuffer);
			}
			
			//if( false )
			{
				sendPacket.clear();
				String str = "earth";
				sendPacket.putInt(0x0002);
				sendPacket.put(cs.encode(str));
				int nSendSize = sendPacket.position();
				sendPacket.flip();
				
				//
				sendBuffer.clear();
				sendBuffer.putInt(nSendSize);
				//sendBuffer.put(sendPacket.array(), 0, nSendSize);
				sendBuffer.flip();
				
				System.out.println("send 3-1: " + sendBuffer);
				nioSocket.write(sendBuffer);
				
				sendBuffer.clear();
				sendBuffer.put(sendPacket.array(), 0, nSendSize);
				sendBuffer.flip();
				
				Scanner sc = new Scanner(System.in);
				sc.next();

				System.out.println("send 3-2: " + sendBuffer);
				nioSocket.write(sendBuffer);
			}

			{
				Scanner sc = new Scanner(System.in);
				sc.next();
			}
			
		}
		catch( Exception e )
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
			e.printStackTrace();
		}
		
	}
}