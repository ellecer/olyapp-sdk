package org.olyapp.sdk.lvsrv;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.olyapp.sdk.utils.StringUtils;

public class ImageBuffer {

	private static int MAX_PACKET_ID = 0xFFFF;

	byte[] metadata;
	Map<Integer,Packet> packets = new TreeMap<Integer, Packet>();
	Set<Integer> missingIds;
	private int firstPacketId, lastPacketId;

	public ImageBuffer() {
		firstPacketId = -1;
		lastPacketId = -1;
		missingIds = null;
		metadata = null;
	}

	public boolean addPacket(int type, int id, int imageId, byte[] data, int length) throws InvalidPacket {
		PacketType etype = PacketType.parseCode(type);
		int startingPoint = etype.getImageStartingPint(data,length);
		int endingPoint = etype.getImageEndingPint(data,length);
		
		//System.out.println("IPacket type=" + etype.toString() + "; id=" + id + "; image: " + StringUtils.toHex(imageId) + "; start=" + startingPoint + "; end=" + endingPoint);
		
		if (etype==PacketType.START) {
			metadata = new byte[startingPoint-1];
			System.arraycopy(data, 12, metadata, 0, startingPoint-1);
		}
		
		packets.put(id,new Packet(data,startingPoint,endingPoint));
		
		if (etype==PacketType.START) {
			firstPacketId = id;
		} else if (etype==PacketType.END) {
			lastPacketId = id;
		}

		if (firstPacketId!=-1 && lastPacketId!=-1) {
			if (missingIds==null) {
				missingIds = new TreeSet<Integer>();
				Set<Integer> packetIds = packets.keySet();
				int normLastPacketId = firstPacketId<lastPacketId ? lastPacketId : lastPacketId + MAX_PACKET_ID + 1;
				
				//System.out.println("packets from: " + firstPacketId + " to " + normLastPacketId);
				for (int i=firstPacketId ; i<=normLastPacketId ; i++) {
					int mod = i % (MAX_PACKET_ID+1);
					if (!packetIds.contains(mod)) {
						missingIds.add(mod);
					}
				}

				if (missingIds.size()>0) {
					System.out.println("Out of order packets in image " + StringUtils.toHex(imageId));
				}
				
			} else {
				missingIds.remove(id);
			}
			
			if (missingIds.size()>0) {
				System.out.println("Missing ids in image " +  StringUtils.toHex(imageId) + " are: " + missingIds.toString());
			}
		}

		return missingIds!=null && missingIds.size()==0;
	}

	public byte[] getImage() {
		int size = 0;
		for (Packet p: packets.values()) {
			int packetDataSize = p.getData().length; 
			size+=packetDataSize;
		}
		byte[] newBuff = new byte[size];
		int offset = 0;
		for (Packet p: packets.values()) {
			int length = p.getData().length;
			System.arraycopy(p.getData(),0,newBuff,offset,length);
			offset+=length;
		}
		return newBuff;
	}

	public byte[] getMetadata() {
		return metadata;
	}
	
}