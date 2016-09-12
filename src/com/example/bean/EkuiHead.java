package com.example.bean;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import struct.StructClass;
import struct.StructField;
@StructClass
public class EkuiHead {
	@StructField(order = 0)
	byte sync;
	@StructField(order = 1)
	byte version;
	@StructField(order = 2)
	byte flag;
	@StructField(order = 3)
	byte reservedByte;
	@StructField(order = 4)
	short command;
	@StructField(order = 5)
	short req;
	@StructField(order = 6)
	int payload_len;
	@StructField(order = 7)
	int reserved;
	public byte getSync() {
		return sync;
	}
	public void setSync(byte sync) {
		this.sync = sync;
	}
	public byte getVersion() {
		return version;
	}
	public void setVersion(byte version) {
		this.version = version;
	}
	public byte getFlag() {
		return flag;
	}
	public void setFlag(byte flag) {
		this.flag = flag;
	}
	public byte getReservedByte() {
		return reservedByte;
	}
	public void setReservedByte(byte reservedByte) {
		this.reservedByte = reservedByte;
	}
	public short getReq() throws IOException {
		return LittleEndian2BigEndian16(req);
	}
	public void setReq(short req) throws IOException {
		this.req = BigEndian2LittleEndian16(req);	
	}
	public int getReserved() {
		return reserved;
	}
	public void setReserved(int reserved) {
		this.reserved = reserved;
	}
	public short getCommand() throws IOException {
		return LittleEndian2BigEndian16(command);
	}
	public void setCommand(short command) throws IOException {
		this.command = BigEndian2LittleEndian16(command);	
	}
	public int getPayload_len() throws IOException {
		return LittleEndian2BigEndian32(payload_len);
	}
	public void setPayload_len(int payload_len) throws IOException {
		this.payload_len = BigEndian2LittleEndian32(payload_len);
	}
	public EkuiHead() {
		super();
		sync = (byte) 0xa5;
		reserved = 0;
		flag = 0;
		reservedByte = 0;
		version = (byte)0x01;
	}

	public EkuiHead(boolean isEmpty){
		
	}
	
	private final short BigEndian2LittleEndian16(short x) throws IOException {  
		ByteBuffer bb = ByteBuffer.wrap(new byte[2]);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.asShortBuffer().put(x);
		ByteArrayInputStream bintput = new ByteArrayInputStream(bb.array());
		DataInputStream dShortPut = new DataInputStream(bintput);
		short y = dShortPut.readShort();
		bintput.close();
		dShortPut.close();
		return y;
		
	}  
	
	private final short LittleEndian2BigEndian16(short x) throws IOException{
		ByteBuffer bb = ByteBuffer.wrap(new byte[2]);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.asShortBuffer().put(x);
		ByteArrayInputStream bintput = new ByteArrayInputStream(bb.array());
		DataInputStream dShortPut = new DataInputStream(bintput);
		short y = dShortPut.readShort();
		bintput.close();
		dShortPut.close();
		return y;
	}
	
	private final int BigEndian2LittleEndian32(int x) throws IOException {  
		ByteBuffer bb = ByteBuffer.wrap(new byte[4]);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.asIntBuffer().put(x);
		ByteArrayInputStream bintput = new ByteArrayInputStream(bb.array());
		DataInputStream dIntPut = new DataInputStream(bintput);
		int y = dIntPut.readInt();
		bintput.close();
		dIntPut.close();
		return y;
	}  

	private final int LittleEndian2BigEndian32(int x) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(new byte[4]);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.asIntBuffer().put(x);
		ByteArrayInputStream bintput = new ByteArrayInputStream(bb.array());
		DataInputStream dintput = new DataInputStream(bintput);
		int y = dintput.readInt();
		bintput.close();
		dintput.close();
		return y;
	}

	public static enum EkuiCmd{
		LOGIN((short)0xf0),
		LOGOUT((short)0xf1),
		PUSH((short)0xf2),
		REQ((short)0xf3),
		NULL_CMD((short)0xff);
		;
		short val;
		private EkuiCmd(short val) {
			this.val = val;
		}
		public static EkuiCmd getCmd(short val){
			switch (val) {
			case (short)0xf0:
				return LOGIN;
			case (short)0xf1:
				return LOGOUT;
			case (short)0xf2:
				return PUSH;
			case (short)0xf3:
				return REQ;
			default:
				break;
			}
			return NULL_CMD;
		}
		public short getVal(){
			return val;
		}
	}
	
}
