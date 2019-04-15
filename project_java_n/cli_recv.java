import java.io.*;
import java.nio.*;
import java.net.*;
import java.lang.Thread;
import java.nio.channels.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.*;
import java.util.*;

public class cli_recv extends Thread
{
	Socket socket = null;
	SocketChannel nioSocket = null;
	
	InputStream is = null;
	DataInputStream dis = null;
	
	ByteBuffer recvBuffer = ByteBuffer.allocate(1024*10);
	ByteBuffer recvPacket = ByteBuffer.allocate(1024);
	
	Charset cs = Charset.forName("UTF-8");
	
	cli_recv(Socket socket)
	{
		this.socket = socket;
	}
	
	cli_recv(SocketChannel nioSocket)
	{
		this.nioSocket = nioSocket;
	}
	
	public void run()
	{
		try
		{
			while( true )
			{
				int byteCount = nioSocket.read(recvBuffer);
				if( -1 == byteCount )
					return;

				//System.out.println("## recvBuffer: " + recvBuffer);
				
				//
				int offset = 0;
				int tail = recvBuffer.position();
				int remain = tail;
				
				while( true )
				{
					remain = tail - offset;
					if( 4 > remain )
						break;
					
					int len = recvBuffer.getInt(offset);
					offset += 4;
					if( len > remain )
						break;
					
					int protocol = recvBuffer.getInt(offset);
					ByteBuffer recvPacket = ByteBuffer.allocate(len);
					recvPacket.put(recvBuffer.array(), 0, len);
					recvPacket.flip();

					offset += (len - 4);

					//
					switch( protocol )
					{
					case 0x0001:
						{
							reqConnection packet = new reqConnection();
							packet.setPacketData(recvPacket, len);
							
							System.out.println("-> regist: " + packet.getName());
						}
						break;
					case 0x0002:
						{
							reqMsg_All packet = new reqMsg_All();
							packet.setPacketData(recvPacket, len);
							
							System.out.println("-> " + packet.getMsg());
						}
						break;
					case 0x0003:
						{
							reqMsg_Any packet = new reqMsg_Any();
							packet.setPacketData(recvPacket, len);
							
							System.out.println("-> " + packet.getMsg());
						}
						break;
					}
				}

				//
				recvBuffer.position(tail - remain);
				recvBuffer.compact();
				recvBuffer.position(remain);
				//System.out.println("## recvBuffer: " + recvBuffer);
			}
		}
		catch( SocketException sockExc )
		{
			System.out.println("disconnect.");
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
		
	}
}