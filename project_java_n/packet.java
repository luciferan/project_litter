import java.nio.ByteBuffer;
import java.nio.charset.*;

//
/*
packetStruct
{
	dataSize; // 4byte = 4 + 4 + nByte

	{
		protocol; // 4byte
		data; // nByte
	}
}
*/
abstract class packetStruct
{
	int dataSize = 0;
	ByteBuffer dataBuffer;

	//
	int getDataSize() { return dataSize; }
	
	boolean setData(ByteBuffer dataBuffer, int dataSize) 
	{
		this.dataSize = dataSize; 
		this.dataBuffer = ByteBuffer.allocate(dataSize);
		this.dataBuffer.put(dataBuffer.array(), 0, dataSize);
		
		if( dataSize != dataBuffer.position() )
			return false;
		return true;
	}
	
	ByteBuffer getData() 
	{
		return dataBuffer; 
	}

	//
	abstract boolean makePacket();
	abstract boolean parsePacket();
	
	boolean setPacketData(ByteBuffer buffer, int dataSize)
	{
		setData(buffer, dataSize); parsePacket(); dataBuffer.flip(); return true; 
	}
	ByteBuffer getPacketData() 
	{
		makePacket(); dataBuffer.flip(); return dataBuffer; 
	}
};

class reqConnection extends packetStruct
{
	int protocol = 0x0001;
	
	String name;
	short nameLen = 0;
	
	//
	int getProtocol() { return protocol; }
	
	void setName(String name) { this.name = name; this.nameLen = (short)name.length(); }
	String getName() { return name; }
	
	boolean makePacket()
	{
		Charset cs = Charset.forName("UTF-8");

		//
		dataSize = 0;
		dataSize += 4; // packet data length
		dataSize += 4; // protocol
		dataSize += 2; // namelength
		dataSize += name.length();
		
		//
		dataBuffer = ByteBuffer.allocate(dataSize);
		
		dataBuffer.clear();
		dataBuffer.putInt(dataSize); // 4
		dataBuffer.putInt(protocol); // 4
		dataBuffer.putShort(nameLen); // 2
		dataBuffer.put(cs.encode(name));
		
		// dataBuffer.flip();
		
		return true;
	}
	
	boolean parsePacket()
	{
		Charset cs = Charset.forName("UTF-8");
		
		//
		dataBuffer.flip();
		
		dataSize = dataBuffer.getInt();
		protocol = dataBuffer.getInt();
		nameLen = dataBuffer.getShort();
		ByteBuffer temp = ByteBuffer.allocate(nameLen);
		temp.put(dataBuffer.array(), dataBuffer.position(), nameLen);
		temp.flip();
		name = cs.decode(temp).toString();
		
		return true;
	}
};

class reqMsg_All extends packetStruct
{
	int protocol = 0x0002; 
	
	String msg;
	short msgLen;
	
	//
	int getProtocol() { return protocol; }
	
	void setMsg(String msg) { this.msg = msg; this.msgLen = (short)msg.length(); }
	String getMsg() { return msg; }

	boolean makePacket()
	{
		Charset cs = Charset.forName("UTF-8");

		//
		dataSize = 0;
		dataSize += 4; // packet data length
		dataSize += 4; // protocol
		dataSize += 2; // msg length
		dataSize += msg.length();
		
		//
		dataBuffer = ByteBuffer.allocate(dataSize);
		
		dataBuffer.clear();
		dataBuffer.putInt(dataSize); // 4
		dataBuffer.putInt(protocol); // 4
		dataBuffer.putShort(msgLen); // 2
		dataBuffer.put(cs.encode(msg));
		
		// dataBuffer.flip();
		
		return true;
	}
	
	boolean parsePacket()
	{
		Charset cs = Charset.forName("UTF-8");
		
		//
		dataBuffer.flip();
		
		dataSize = dataBuffer.getInt();
		protocol = dataBuffer.getInt();
		msgLen = dataBuffer.getShort();
		ByteBuffer temp = ByteBuffer.allocate(msgLen);
		temp.put(dataBuffer.array(), dataBuffer.position(), msgLen);
		temp.flip();
		msg = cs.decode(temp).toString();
		
		return true;
	}
};

class reqMsg_Any extends packetStruct
{
	int protocol = 0x0003; 
	
	String msg;
	short msgLen;
	
	//
	int getProtocol() { return protocol; }
	
	void setMsg(String msg) { this.msg = msg; this.msgLen = (short)msg.length(); }
	String getMsg() { return msg; }

	boolean makePacket()
	{
		Charset cs = Charset.forName("UTF-8");

		//
		dataSize = 0;
		dataSize += 4; // packet data length
		dataSize += 4; // protocol
		dataSize += 2; // msg length
		dataSize += msg.length();
		
		//
		dataBuffer = ByteBuffer.allocate(dataSize);
		
		dataBuffer.clear();
		dataBuffer.putInt(dataSize); // 4
		dataBuffer.putInt(protocol); // 4
		dataBuffer.putShort(msgLen); // 2
		dataBuffer.put(cs.encode(msg));
		
		// dataBuffer.flip();
		
		return true;
	}
	
	boolean parsePacket()
	{
		Charset cs = Charset.forName("UTF-8");
		
		//
		dataBuffer.flip();
		
		dataSize = dataBuffer.getInt();
		protocol = dataBuffer.getInt();
		msgLen = dataBuffer.getShort();
		ByteBuffer temp = ByteBuffer.allocate(msgLen);
		temp.put(dataBuffer.array(), dataBuffer.position(), msgLen);
		temp.flip();
		msg = cs.decode(temp).toString();
		
		return true;
	}
};

//
class packet_main
{
	public static void main(String args[])
	{
		String name = "0001";
		
		//
		reqConnection sendPacket = new reqConnection();
		sendPacket.setName(new String("cli_001"));
		sendPacket.makePacket();
		
		System.out.println("encode sendPacket: " + sendPacket.getData());
		
		//
		int recvDataSize = sendPacket.getDataSize();
		ByteBuffer recvBuffer = ByteBuffer.allocate(recvDataSize);
		recvBuffer.put(sendPacket.getData().array(), 0, recvDataSize);
		System.out.println("receive packet: " + recvBuffer);
		
		//
		reqConnection recvPacket = new reqConnection();
		recvPacket.setData(recvBuffer, recvDataSize);
		
		recvPacket.parsePacket();
		System.out.println("decode recvPacket: " + recvPacket.getName());
	}
}
