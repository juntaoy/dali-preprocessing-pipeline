package dali.data;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import is2.data.SentenceData09;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class SentenceData {
	
	private ArrayList<String> token;
	private ArrayList<String> pos;
	private ArrayList<String> label;
	private ArrayList<Integer> head;
	private HashSet<MarkableData> markables;
	
	private static final String[] pennBracket={"-LRB-","-RRB-","-LSB-","-RSB-","-LCB-","-RCB-","`","``","''"};
	private static final String[] strBracket={"(",")","[","]","{","}","'","\"","\""} ;
	
	private static final String non_link_func="cop|aux|mark|expl|neg|parataxis|csubj|nsubj|cc|punct|acomp|discourse|advcl|dep";
	
	private static final String nn_ex_label = "advmod|partmod|prep|cc|nn|discourse|punct|amod|num|det|cop|aux";
	private static final String prp_ex_label = "punct|cc|advmod";
	private static final String ex_in_label = "expl";
	private static final String cd_in_label = "nsubj|nsubjpass|pobj|dobj|poss|ROOT|appos|acomp";
	private static final String dt_in_label = "nsubj|nsubjpass|pobj|dobj|conj|npadvmod";
	private static final String jj_in_label = "nsubj|nsubjpass|dobj|appos";
	private static final String vb_in_label = "nsubj|nsubjpass|dobj|pobj";
	private static final String doller_in_label="dep|dobj|pobj";
	
	
	public SentenceData(DataInputStream dis)throws Exception{
		this();
		int size = dis.readInt();
		for(int i=0;i<size;i++)
			token.add(dis.readUTF());
		
		size = dis.readInt();
		for(int i=0;i<size;i++)
			pos.add(dis.readUTF());
		
		size = dis.readInt();
		for(int i=0;i<size;i++)
			label.add(dis.readUTF());
		
		size = dis.readInt();
		for(int i=0;i<size;i++)
			head.add(dis.readInt());
		
		size = dis.readInt();
		for(int i=0;i<size;i++)
			markables.add(new MarkableData(dis));
		
	}
	
	
	public SentenceData(){
		this.token = new ArrayList<String>();
		this.pos = new ArrayList<String>();
		this.label = new ArrayList<String>();
		this.head = new ArrayList<Integer>();
		this.markables=new HashSet<MarkableData>();
		token.add("ROOT");
		pos.add("<ROOT-POS>");
		label.add("root-label");
		head.add(-1);
	}
	
	
	
	public void writeCoNLL0609Data(BufferedWriter writer, boolean is09)throws Exception{
		if(is09){
			for(int i=1;i<token.size();i++){
				String tok = getPennTokenById(i);
				String lem = tok.toLowerCase();
				String pos = getPosById(i);
				String label = getLabelById(i);
				int head = getHeadById(i);
				writer.write(i+"\t"+tok+"\t"+lem+"\t"+lem+"\t"+pos+"\t"+pos+"\t_\t_\t"+head+"\t"+head+"\t"+label+"\t"+label+"\t_\t_\n");
			}
		}else{
			for(int i=1;i<tokSize();i++)
				writer.write(i+"\t"+getPennTokenById(i)+"\t_\t"+getPosById(i)+"\t"+getPosById(i)+"\t_\t"+getHeadById(i)+"\t"+getLabelById(i)+"\t_\t_\n");
		}
		writer.newLine();
	}
	
	
	public void writeData(DataOutputStream dos)throws Exception{
		dos.writeInt(token.size()-1);
		for(int i=1;i<token.size();i++)
			dos.writeUTF(token.get(i));
		
		dos.writeInt(pos.size()-1);
		for(int i=1;i<pos.size();i++)
			dos.writeUTF(pos.get(i));
		
		dos.writeInt(label.size()-1);
		for(int i=1;i<label.size();i++)
			dos.writeUTF(label.get(i));
		
		dos.writeInt(head.size()-1);
		for(int i=1;i<head.size();i++)
			dos.writeInt(head.get(i));
		
		dos.writeInt(markables.size());
		for(MarkableData mark:markables)
			mark.writeData(dos);
	}
	
	
	
	public void addTokens(String tok){
		token.add(bracketPennToString(tok));
	}
	
	public void addPos(String pos){
		this.pos.add(pos);
	}
	
	public void addLabel(String lab){
		this.label.add(lab.toLowerCase());
	}
	
	public void addHead(int h){
		this.head.add(h);
	}
	
	public void resetMarkableHeadIdByDep(){
		if(token.size()==head.size()){
			for(MarkableData mark:markables){
				int s = mark.getStart();
				int e = mark.getEnd();
				for(int i=s;i<=e;i++){
					int h = head.get(i);
					if(h<s||h>e)
						mark.setHead(i);
				}
			}
		}
	}
	

	@Override
	public String toString() {
		
		return "SentenceData:" + getStrMarkables(markables);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<MarkableData> getListMarkables(){
		ArrayList<MarkableData> marks = new ArrayList<MarkableData>();
		for(MarkableData mark:markables)
			marks.add(mark);
		Collections.sort(marks);
		return marks;
	}
	
	public HashSet<MarkableData> getMarkables(){
		return markables;
	}
	
	public String getXmlTokenByMarkable(MarkableData mark){
		StringBuilder sb = new StringBuilder();
		for(int i=mark.getStart();i<=mark.getEnd();i++)
			sb.append(" "+getXmlTokenById(i));
		return sb.toString().trim();
	}
	
	public String getXmlTokenById(int id){
		if(id>=token.size()||id<0)
			return null;
		return convertXMLchars(token.get(id));
	}
	
	public String getPennTokenById(int id){
		if(id>=token.size()||id<0)
			return null;
		return bracketStringToPenn(token.get(id));
	}
	
	public String getTokenById(int id){
		if(id>=token.size()||id<0)
			return null;
		return token.get(id);
	}

	public String[] getToken(){
		String[] tok = new String[token.size()];
		for(int i=0;i<token.size();i++)
			tok[i]=token.get(i);
		return tok;
	}
	
	public String getSentenceStr(){
		StringBuilder sb = new StringBuilder();
		for(int i=1;i<token.size();i++)
			sb.append(" "+token.get(i));
		return sb.toString().trim();
	}
	
	public String getPosByMarkable(MarkableData mark){
		StringBuilder sb = new StringBuilder();
		for(int i=mark.getStart();i<=mark.getEnd();i++)
			sb.append(" "+getPosById(i));
		return sb.toString().trim();
	}
	
	public String getPosById(int id){
		if(id>=pos.size()||id<0)
			return null;
		return pos.get(id);
	}
	
	public String[] getPos(){
		String[] p = new String[pos.size()];
		for(int i=0;i<pos.size();i++)
			p[i]=pos.get(i);
		return p;
	}
	
	public String getLabelById(int id){
		if(id>=label.size()||id<0)
			return null;
		return label.get(id);
	}
	public String[] getLabel(){
		String[] l = new String[label.size()];
		for(int i=0;i<label.size();i++)
			l[i]=label.get(i);
		return l;
	}
	
	public int getHeadById(int id){
		if(id>=head.size()||id<0)
			return -1;
		return head.get(id);
	}
	
	public int[] getHead(){
		int[] h = new int[head.size()];
		for(int i=0;i<head.size();i++)
			h[i]=head.get(i);
		return h;
	}
	
	public int tokSize(){
		return token.size();
	}
	
	public String getStrMarkables(HashSet<MarkableData> markables){
		StringBuilder sb = new StringBuilder();
		boolean[][] heads = new boolean[token.size()][this.markables.size()];
		int[][] marks = new int[token.size()][token.size()];
		int[] tomapid = new int[this.markables.size()];
		for(int i=0;i<marks.length;i++)
			Arrays.fill(marks[i], -1);
		//sb.append("No_Head:");
		for(MarkableData mk:markables){
			int h=mk.getHead();
			int id =mk.getId();
			if(h>=0)
				heads[h][id]=true;
			//else
			//	sb.append(" "+mk.mapid);
			marks[mk.getStart()][mk.getEnd()]=id;
			tomapid[id]=mk.getMapid();
		}
		//sb.append("\n");
		for(int i=1;i<token.size();i++){
			if(i!=1)sb.append(" ");
			
			for(int m=marks[i].length-1;m>0;m--)
				if(marks[i][m]>=0)
					//sb.append("[");
					sb.append("["+tomapid[marks[i][m]]+" ");
			for(int h=0;h<heads[i].length;h++)
				if(heads[i][h])	
					sb.append(tomapid[h]+"*");
			
			sb.append(token.get(i));
			
			for(int m=marks[i].length-1;m>0;m--)
				if(marks[m][i]>=0)
					//sb.append("]");
					sb.append(" "+tomapid[marks[m][i]]+"]");
			
		}
		return sb.toString();
	}
	
	
	public  void addMarkables(SentenceData09 ins){
		//always replace POS for Mate parser
		pos.clear();
		pos.add("<ROOT-POS>");
		for(int i=1;i<ins.forms.length;i++){
			//replace pos 
			addPos(ins.ppos[i]);
			
			addHead(ins.pheads[i]);
			addLabel(ins.plabels[i]);
			String p = ins.ppos[i];
			String l = ins.plabels[i];
			if(isPRP(p,l)){
				addMarkables(i,i,i);
			}else if(isHead(p,l)){
				int start = getMarkableByHead(ins.pheads,ins.plabels,i,true);
				int end = getMarkableByHead(ins.pheads,ins.plabels,i,false);
				if(!(i>1&&p.startsWith("NN")&&l.equals("conj")&&ins.ppos[i-1].matches("CC|,")))
					addMarkables(start, end,i);
				
			}
		}
		mergeCrossMentions();
	}
	
	
	public void addMarkables(int start, int end){
		addMarkables(start,end,-1);//no head word
	}
	
	public void addMarkables(int start, int end,int head){
		MarkableData mark = new MarkableData(markables.size(),start,end,head);
		if(!markables.contains(mark))
			markables.add(mark);
	}
	
	public void cleanMarkables(){
		markables.clear();
	}
	
	public int markableSize(){
		return this.markables.size();
	}
	
	public boolean containMarkable(MarkableData pdmark){
		return markables.contains(pdmark);
	}
	
	private void updateMarkId(){
		int i=0;
		for(MarkableData m:markables){
			m.setId(i++);
			m.setMapid(m.getId());
		}
	}
	
	
	private void mergeCrossMentions(){
		HashSet<MarkableData> toremove = new HashSet<MarkableData>();
		HashSet<MarkableData> toadd = new HashSet<MarkableData>();
		for(MarkableData m1:markables){
			for(MarkableData m2:markables){
				if(m1==m2)
					continue;
				if(m1.getStart()<m2.getStart()&&m1.getEnd()>m2.getStart()&&m1.getEnd()<m2.getEnd()){
					toremove.add(m1);
					toremove.add(m2);
					MarkableData mt = new MarkableData(m2.getId(),m1.getStart(),m2.getEnd(),m2.getHead());
					toadd.add(mt);
				}
			}
		}
		
		markables.removeAll(toremove);
		markables.addAll(toadd);
		updateMarkId();
		
	}
	
	
	private boolean isPRP(String p,String l){
		return p.startsWith("PRP")&&!l.matches(prp_ex_label);
	}
		
	private boolean isHead(String p, String l){
		if(p.startsWith("NN")&&!l.matches(nn_ex_label))
			return true;
		if(p.equals("CD")&&l.matches(cd_in_label))
			return true;
		if(p.equals("DT")&&l.matches(dt_in_label))
			return true;
		if(p.equals("JJ")&&l.matches(jj_in_label))
			return true;
		if(p.equals("EX")&&l.matches(ex_in_label))
			return true;
		if(p.startsWith("VB")&&l.matches(vb_in_label))
			return true;
		if(p.equals("$")&&l.matches(doller_in_label))
			return true;
		return false;
	}
	    
	
	
	
	private int getMarkableByHead(int[] heads,String[] labels, int h, boolean isleft){
		int index = h;

		if(isleft){
			for(int i=1;i<h;i++){
				if(heads[i]==h){
					if(labels[i].matches(non_link_func))
						continue;
					index = getMarkableByHead(heads,labels,i,isleft);
					break;
				}
			}
		}else{
			for(int i=heads.length-1;i>h;i--){
				if(heads[i]==h){
					if(labels[i].matches(non_link_func))
						continue;
					index = getMarkableByHead(heads,labels,i,isleft);
					break;
				}
			}
		
		}
		
		return index;
		
	}
	
	private String bracketStringToPenn(String tok){
		for(int i=0;i<pennBracket.length;i++)
			if(tok.equals(strBracket[i]))
				return pennBracket[i];
		return tok;
	}
	
	private String bracketPennToString(String tok){
		for(int i=0;i<pennBracket.length;i++)
			if(tok.equals(pennBracket[i]))
				return strBracket[i];
		return tok;
	}
	
	
	/**
	 * taken from Anawiki.CharniakXML.CharniakXML.java
	 * @param word
	 * @return
	 */
    private String convertXMLchars(String word){
        if ((word.indexOf("&")) >= 0)
            word = word.replaceAll("\\&","&amp;");
        if ((word.indexOf("<")) >= 0);
            word = word.replaceAll("\\<","&lt;");
        if ((word.indexOf(">")) >= 0)
            word = word.replaceAll("\\>","&gt;");
        if ((word.indexOf("\"")) >= 0)
 		   word = word.replaceAll("\"","&quot;");
 	   if ((word.indexOf("\'")) >= 0)
            word = word.replaceAll("\'","&apos;");
 	   
        return word;
     }
	

}
