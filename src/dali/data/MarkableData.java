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
	private int category;
	private int set_id;
	private int head_end;
	private int ner;
	private int last_nn;
	
	public MarkableData(DataInputStream dis) throws Exception{
		this.id = dis.readInt();
		this.mapid = dis.readInt();
		this.head = dis.readInt();
		this.start = dis.readInt();
		this.end = dis.readInt();
		this.category = dis.readInt();
		this.set_id = dis.readInt();
		this.head_end = dis.readInt();
		this.ner = dis.readInt();
		this.last_nn=dis.readInt();
	}
	
	public MarkableData(int id, int start, int end, int head, int category, int set_id,int head_end,int ner,int last_nn){
		this.id=id;
		this.mapid=id;
		this.start = start;
		this.end = end;
		this.head=head;
		if(head<0&&end-start<=2)
			this.head=end;
		this.category = category;
		this.set_id = set_id;
		this.head_end = head_end;
		this.ner = ner;
		this.last_nn = last_nn;
	}
	
	public MarkableData(int id,int start,int end, int head,int category,int set_id){
		this(id,start,end,head,category,set_id,-1,-1,-1);
	}
	
	public MarkableData(int id, int start,int end,int head,int ner){
		this(id,start,end,head,-1,-1,-1,ner,-1);
	}
	
	public MarkableData(int id, int start,int end,int head){
		this(id,start,end,head,-1,-1);
	}
	
	public void writeData(DataOutputStream dos)throws Exception{
		dos.writeInt(id);
		dos.writeInt(mapid);
		dos.writeInt(head);
		dos.writeInt(start);
		dos.writeInt(end);
		dos.writeInt(category);
		dos.writeInt(set_id);
		dos.writeInt(head_end);
		dos.writeInt(ner);
		dos.writeInt(last_nn);
	}
	
	public int getNer(){
		return ner;
	}
	
	public void setNer(int ner){
		this.ner = ner;
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
	public int getHeadEnd(){
		return head_end;
	}
	public int getMostLikelyHead(){
		//head_end > head > last_nn > end
		if(head_end>=0)
			return head_end;
		else if(head>=0)
			return head;
		else if(last_nn>=0)
			return last_nn;
		return end;
	}
	public void setHeadEnd(int head_end){
		this.head_end = head_end;
	}
	public void setHead(int head){
		this.head=head;
	}
	
	public int getCategory(){
		return category;
	}
	
	public void setCategory(int category){
		this.category = category;
	}
	
	public int getSetId(){
		return set_id;
	}
	public void setSetId(int set_id){
		this.set_id = set_id;
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
				+ head + ", start=" + start + ", end=" + end + ", category="+category+",set_id"+set_id+"]";
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
