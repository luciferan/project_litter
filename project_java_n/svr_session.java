import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.*;

import java.util.*;
import java.util.LinkedList;

//
public class svr_session implements Runnable
{
	long index = -1;
	
	SocketChannel nioSocket = null;
	Selector selector = null;
	
	svr_dbProc dbConn = null;
	
	Charset cs = Charset.forName("UTF-8");

	ByteBuffer recvBuffer;
	LinkedList<ByteBuffer> recvQueue = new LinkedList<>();
	private Object recvLock = new Object();

	ByteBuffer sendBuffer;
	LinkedList<ByteBuffer> sendQueue = new LinkedList<>();
	private Object sendLock = new Object();

	svr_dataManager dataManager = null;
	
	Thread sendThread = new Thread(new svr_sendThread(this));
	Thread recvThread = new Thread(new svr_recvThread(this));
	
	Thread getSendThread() { return sendThread; }
	Thread getRecvThread() { return recvThread; }
	
	svr_log log = new svr_log("svr_log.log");
	// File file = new File("svr_log.log");

	//
	String name;

	//
	svr_session(long index, SocketChannel nioSocket, Selector selector, svr_dataManager dataManager) throws IOException
	{
		this.index = index;
		this.nioSocket = nioSocket;
		this.selector = selector;
		this.dataManager = dataManager;
		// this.dbConn = new svr_dbProc();

		nioSocket.configureBlocking(false);
		SelectionKey key = nioSocket.register(selector, SelectionKey.OP_READ);
		
		key.attach(this);
		
		recvBuffer = ByteBuffer.allocate(1024*10);
		sendBuffer = ByteBuffer.allocate(1024*10);
	}
	
	long getIndex() { return index; }

	SocketChannel getNIOSocket() { return nioSocket; }
	//void setSocket(SocketChannel nioSocket) { this.nioSocket = nioSocket; }
	
	void setDataManager(svr_dataManager dataManager) { this.dataManager = dataManager; }
	
	//
	void disconnect()
	{
		System.out.println(getIndex() + ": disconnect().");
		try
		{
			if( null != nioSocket && nioSocket.isOpen() )
				nioSocket.close();
			else
				System.out.println(getIndex() + ": already disconnect.");
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	int onRecv()
	{
		//System.out.println(getIndex() + ": onRecv(): recvBuffer: " + recvBuffer);

		try
		{
			synchronized( recvLock )
			{
				int byteCount = nioSocket.read(recvBuffer);
				//System.out.println(getIndex() + ": onRecv(): read byte " + byteCount);
				if( -1 == byteCount )
				{
					System.out.println(getIndex() + ": read(-1).");
					throw new IOException();
				}

				// packet struct [Length:4byte][packet:[protocol:4byte][data:nbyte]] : length = 4 + packetlength 

				// data parsing
				int offset = 0;
				int head = 0;
				int tail = recvBuffer.position();
				int remain = tail - offset;

				while( true )
				{
					remain = tail - offset;
					
					//System.out.println(getIndex() + ": offset: " + offset + ", tail: " + tail + ", remain: " + remain);
					if( 4 > remain )
						break;
					
					int len = recvBuffer.getInt(offset);
					if( len > remain )
						break;
					
					ByteBuffer recvPacket = ByteBuffer.allocate(len);
					recvPacket.put(recvBuffer.array(), offset, len);
					offset += len;
					
					synchronized( recvQueue )
					{
						recvQueue.push(recvPacket);
					}
				}
				
				// recv buffer compact
				{
					//System.out.println(getIndex() + ": recvBuffer: " + recvBuffer);
					recvBuffer.position(tail - remain);
					recvBuffer.compact();
					recvBuffer.position(remain);
					//System.out.println(getIndex() + ": recvBuffer: " + recvBuffer);
				}
			}
						
			return 0;
		}
		catch( Exception e )
		{
			return -1;
		}
	}
	
	int procRecv()
	{
		System.out.println(getIndex() + ": procRecv()");
		
		try
		{
			synchronized( recvLock )
			{
				while( 0 < recvQueue.size() )
				{
					ByteBuffer recvData = recvQueue.pop();
					recvData.flip();
					
					int len = recvData.getInt();
					int protocol = recvData.getInt();
					
					if( -1 == procMsg(protocol, len, recvData) )
					{
						disconnect();
						return -1;
					}
				}
			}

			// continue data receive
			SelectionKey key = nioSocket.keyFor(selector);
			if( SelectionKey.OP_WRITE != key.interestOps() )
			{
				key.interestOps(SelectionKey.OP_READ);
				selector.wakeup();
			}
			
			return 0;
		}
		catch( Exception e )
		{
			return -1;
		}
	}

	@Override
	public void run()
	{
		System.out.println("## session thread: " + Thread.currentThread().getName());
		procRecv();
	}
	
	int procMsg(int protocol, int recvDataSize, ByteBuffer recvData)
	{
		switch( protocol )
		{
		case 0x0001:
			{
				reqConnection recvPacket = new reqConnection();
				recvPacket.setData(recvData, recvDataSize);
				recvPacket.parsePacket();
				
				System.out.println(getIndex() + ": prot[" + recvPacket.getProtocol() + "] " + recvPacket.getName());
				
				this.name = new String(recvPacket.getName());
				
				//
				{
					String sendStr = recvPacket.getName();
					int strLen = sendStr.length();
					
					reqConnection sendPacket = new reqConnection();
					sendPacket.setName(sendStr);
					sendPacket.makePacket();
					
					System.out.println("## procMsg: protocol: 0x0001 send: " + sendPacket.getDataSize() + " : " + sendPacket.getData());
					SendPacket(sendPacket.getData(), sendPacket.getDataSize());
				}
			}
			break;
		case 0x0002:
			{
				reqMsg_All recvPacket = new reqMsg_All();
				recvPacket.setData(recvData, recvDataSize);
				recvPacket.parsePacket();
				
				System.out.println(getIndex() + ": prot[" + recvPacket.getProtocol() + "] " + recvPacket.getMsg());
				
				//
				{
					String sendMsg = new String(name + ": " + recvPacket.getMsg());
					int msgLen = sendMsg.length();
					
					reqMsg_All sendPacket = new reqMsg_All();
					sendPacket.setMsg(sendMsg);
					sendPacket.makePacket();
					
					//
					SendPacket_All(sendPacket.getData(), sendPacket.getDataSize());
				}
			}
			break;
		case 0x0003:
			{
				reqMsg_Any recvPacket = new reqMsg_Any();
				recvPacket.setData(recvData, recvDataSize);
				recvPacket.parsePacket();
				
				System.out.println(getIndex() + ": prot[" + recvPacket.getProtocol() + "] " + recvPacket.getMsg());
				
				//
				{
					String sendMsg = new String(name + ": " + recvPacket.getMsg());
					int msgLen = sendMsg.length();
					
					reqMsg_Any sendPacket = new reqMsg_Any();
					sendPacket.setMsg(sendMsg);
					sendPacket.makePacket();
					
					//
					SendPacket_Any(sendPacket.getData(), sendPacket.getDataSize());
				}				
			}
			break;			
		default:
			{
				return -1;
			}
		}
		
		return 0;
	}
	
	//
	int addSendPacket(ByteBuffer sendPacket, int len)
	{
		synchronized( sendQueue )
		{
			sendQueue.push(sendPacket);
			return sendQueue.size();
		}
	}
	
	ByteBuffer getNextSendPacket()
	{
		synchronized( sendQueue )
		{
			if( 0 < sendQueue.size() )
			{
				ByteBuffer buffer = sendQueue.pop();
				return buffer;
			}
			else
			{
				return null;
			}
		}
	}
	
	int onSend()
	{
		writeLog(getIndex() + " : onSend()");
		
		//
		{
			SelectionKey key = nioSocket.keyFor(selector);

			key.interestOps(SelectionKey.OP_READ);
			selector.wakeup();
		}
			
		//
		synchronized( sendLock )
		{
			if( 0 >= sendQueue.size() )
			{
				writeLog(getIndex() + " : onSend(): sendQueueSize " + sendQueue.size());
				return sendBuffer.position();
			}

			int offset = sendBuffer.position();
			int limit = sendBuffer.limit();
			int leftBuffer = limit - offset;

			while( true )
			{
				leftBuffer = limit - offset;
				
				//
				ByteBuffer sendPacket;
				try
				{
					sendPacket = sendQueue.getFirst();
					writeLog(getIndex() + " : onSend(): sendPacket: " + sendPacket);
				}
				catch( NoSuchElementException e )
				{
					writeLog(getIndex() + " : onSend(): NoSuchElementException");
					break;
				}
				
				int len = sendPacket.position();
				if( len > leftBuffer )
					break;
				
				//
				sendPacket.flip();
				sendBuffer.put(sendPacket.array(), 0, len);
				offset += len;
				writeLog(getIndex() + " : onSend(): sendBuffer: " + sendBuffer);
				
				//
				sendQueue.removeFirst();
			}
			
			writeLog(getIndex() + ": onSend() : send request size " + offset);
			return offset;
		}
	}
	
	int procSend()
	{
		writeLog(getIndex() + " : procSend()");
		
		try
		{
			SelectionKey key = nioSocket.keyFor(selector);
			System.out.println(getIndex() + " : procSend(): sendBuffer: " + sendBuffer);
			
			ByteBuffer sendData;
			
			synchronized( sendLock )
			{
				if( 0 >= sendBuffer.position() )
				{
					System.out.println(getIndex() + " : procSend(): empty sendBuffer: ");
					//throw new Exception();
					return 1;
				}
				
				//
				sendData = ByteBuffer.allocate(sendBuffer.position());
				sendData.put(sendBuffer.array(), 0, sendBuffer.position());
				sendBuffer.clear();
				sendData.flip();
				
				writeLog(getIndex() + " : procSend(): make send data : " + sendBuffer + " :: " + sendData);
			}

			writeLog(getIndex() + " : procSend(): begin write: " + sendData);
			while( sendData.hasRemaining() )
			{
				nioSocket.write(sendData);
				writeLog(getIndex() + " : procSend(): after write: " + sendData);
			}
			writeLog(getIndex() + " : procSend(): end write: " + sendData);

			//
			synchronized( sendLock )
			{
				if( 0 < sendQueue.size() )
				{
					writeLog(getIndex() + " : procSend(): left send queue: " + sendQueue.size());
					writeLog(getIndex() + ": procSend(): OP_WRITE");
					key.interestOps(SelectionKey.OP_WRITE);
					selector.wakeup();
				}
				else
				{
					writeLog(getIndex() + " : procSend(): empty send queue");
					key.interestOps(SelectionKey.OP_READ);
					selector.wakeup();
				}
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
			//writeLog(e.getStackTrace());
			return -1;
		}
		
		return 0;
	}
		
	//
	int SendPacket(ByteBuffer sendPacket, int sendPacketSize)
	{
		//System.out.println(getIndex() + ": SendPacket(): " + sendPacketSize + ": " + sendPacket);
		
		//
		//int sendDataSize = sendPacketSize;
		
		//sendBuffer.putInt(sendDataSize);
		//sendBuffer.put(sendPacket.array(), 0, sendDataSize);
		//System.out.println(getIndex() + ": SendPacket(): sendBuffer: " + sendBuffer);
		
		//addSendPacket(sendPacket, sendPacketSize);
		
		synchronized( sendLock )
		{
			writeLog(getIndex() + ": SendPacket(): " + sendPacketSize + ": " + sendPacket);
			sendQueue.push(sendPacket);
			writeLog(getIndex() + ": SendPacket(): sendQueueSize: " + sendQueue.size());
		}
		
		//
		{
			SelectionKey key = nioSocket.keyFor(selector);
			
			writeLog(getIndex() + ": SendPacket(): OP_WRITE");
			key.interestOps(SelectionKey.OP_WRITE);
			selector.wakeup();
		}
		//System.out.println(getIndex() + ": " + key);
		
		//
		return 0;
	}
	
	int SendPacket_All(ByteBuffer sendPacket, int sendPacketSize)
	{
		HashMap<Long, svr_session> sessionList = dataManager.getSessionList();
		
		for( HashMap.Entry<Long, svr_session> iter : sessionList.entrySet() )
		{
			svr_session session = iter.getValue();
			
			//
			session.SendPacket(sendPacket, sendPacketSize);
		}
		
		return 0;
	}
	
	int SendPacket_Any(ByteBuffer sendPacket, int sendPacketSize)
	{
		HashMap<Long, svr_session> sessionList = dataManager.getSessionList();
		
		for( HashMap.Entry<Long, svr_session> iter : sessionList.entrySet() )
		{
			svr_session session = iter.getValue();
			if( this == session )
				continue;
			
			//
			session.SendPacket(sendPacket, sendPacketSize);
		}
		
		return 0;
	}
	
	void writeLog(String str)
	{
		// try
		// {
			// System.out.println(str);
			// FileWriter fw = new FileWriter(file, true);
			// fw.write(str + "\r\n");
			// fw.close();
		// }
		// catch( IOException e )
		// {
			// e.printStackTrace();
		// }
		
		log.writeLog(str);
	}
}

// class packetStruct
// {
	// int length;
	// int protocol;
	// ByteBuffer packetData;
	
	// int setLength(int len) { return this.length = len; }
	// int getLength() { return this.length; }
	// int setProtocol(int protocol) { return this.protocol = protocol; }
	// int getProtocol() { return this.protocol; }
	// void setPacketData(byte data[], int length)
	// {
		// packetData.allocate(length);
		// packetData.put(data, 0, length);
	// }
	// ByteBuffer getPacketData() { return packetData; }
// }