package dali.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class MarkableData implements Comparable{
	private int id;
	private int mapid;
	private int head;
	private int start;
	private int end;
	
	public MarkableData(DataInputStream dis) throws Exception{
		this.id = dis.readInt();
		this.mapid = dis.readInt();
		this.head = dis.readInt();
		this.start = dis.readInt();
		this.end = dis.readInt();
	}
	
	public MarkableData(int id, int start,int end,int head){
		this.id=id;
		this.mapid=id;
		this.start = start;
		this.end = end;
		this.head=head;
		if(head<0&&end-start<=2)
			this.head=end;
	}
	
	public void writeData(DataOutputStream dos)throws Exception{
		dos.writeInt(id);
		dos.writeInt(mapid);
		dos.writeInt(head);
		dos.writeInt(start);
		dos.writeInt(end);
	}
	
	public int getMapid(){
		return mapid;
	}
	public void setMapid(int id){
		mapid=id;
	}
	public int getStart(){
		return start;
	}
	
	public int getEnd(){
		return end;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public int getId(){
		return id;
	}
	
	public int getHead(){
		return head;
	}
	
	public void setHead(int head){
		this.head=head;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + end;
		result = prime * result + start;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MarkableData other = (MarkableData) obj;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}
	
	
	
	@Override
	public String toString() {
		return "MarkableData [id=" + id + ", mapid=" + mapid + ", head="
				+ head + ", start=" + start + ", end=" + end + "]";
	}

	@Override
	public int compareTo(Object o) {
		if (this == o)
			return 0;
		MarkableData other = (MarkableData) o;
		if(start!=other.start)
			return Integer.compare(start, other.start);
		else if(end!=other.end)
			return Integer.compare(end, other.end);
		return 0;
	}

}
