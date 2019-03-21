package dali.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dali.util.Options;
import dali.data.DocumentData;
import dali.data.MarkableData;
import dali.data.SentenceData;
import dali.util.FileHandler;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class MASXML  extends IFormat {

	private static MASXML singleton = new MASXML();
	private static final String SUFFIX = "-masxml.xml";
	private static final String INDENT = Options.INDENT;
	private static final String DTD = "dtd/masxmlpd.dtd";
	private static final String W_POS = "Lpos";
	private static final String W_HEAD = "Lhead";
	private static final String W_REL = "Lrel";
	private static final String NE_REL = "AAgf";
	
	private HashSet<String> stopMarkableId;
	
	
	
	private MASXML(){
		FORMATSUFFIX = SUFFIX;
		NULLSTR="";
	}
	public static IFormat getInstance() {
		return singleton;
	}
	
	
	@Override
	public DocumentData read(File in) throws Exception {
		stopMarkableId = new HashSet<String>();
		DocumentData doc = new DocumentData(in);
		doc.setHeading(getXmlHeading(in));
		
		Document xml = getXmlDocument(in);
		
		NodeList markList = xml.getElementsByTagName("PDante");
		
		for(int i=0;i<markList.getLength();i++){
			Element mar = (Element)markList.item(i);
			if(mar.getAttribute("visibility").equals("hidden"))
				stopMarkableId.add(mar.getAttribute("id"));
			
		}
		NodeList paraNodes = xml.getElementsByTagName("p");
		for(int i=0;i<paraNodes.getLength();i++){
			int cns = 0;
			Node para = paraNodes.item(i);
			NodeList childList = para.getChildNodes();
			for(int j=0;j<childList.getLength();j++){
				Node ch = childList.item(j);
				if(ch.getNodeName().equals("s")){
					cns++;
					
					SentenceData sent = new SentenceData();
					getSentInfo(ch,sent);
					doc.addSentenceData(sent);
					
				}
			}
			doc.addParagraph(cns);
		}
		
		return doc;
	}
	
	
	
	private int getSentInfo(Node node, SentenceData sent){
		NodeList children = node.getChildNodes();
		int start = -1;
		String neId = "";
		int rehead=-1;
		int head=-1;
		for(int i=0;i<children.getLength();i++){
			Node child = children.item(i);
			
			String chName = child.getNodeName();
			if(start>=0){
				if(!stopMarkableId.contains(neId))
					sent.addMarkables(start, sent.tokSize()-1,head);
				start=-1;
				head=-1;
				neId="";
			}
			if(chName.equals("W")){
				Element ech = (Element) child;
				sent.addTokens(child.getTextContent());
				if(!isNullStr(ech.getAttribute(W_POS)))
					sent.addPos(ech.getAttribute(W_POS));
				if(!isNullStr(ech.getAttribute(W_HEAD)))
					sent.addHead(Integer.valueOf(ech.getAttribute(W_HEAD)));
				if(!isNullStr(ech.getAttribute(W_REL)))
					sent.addLabel(ech.getAttribute(W_REL));
			}else if(chName.equals("ne")){
				//the ne will start with next token so no need use size-1;
				start = sent.tokSize();
				neId = ((Element) child).getAttribute("id");
			}else if(chName.equals("nphead")){
				rehead = sent.tokSize();
			}
			
			if(child.hasChildNodes()){
				int phead = getSentInfo(child,sent);
				if(phead>=0)
					head=phead;
			}
				
			
		}
		return rehead;
	}
	
	
	@Override
	public void write(String out, DocumentData doc) throws Exception {
		
		String output = out+FileHandler.FILE_SEPA+doc.getFilename()+SUFFIX;
		List<Integer> paraCnt = doc.getParaCnt();
		List<SentenceData> sentList = doc.getSentList();
		
		BufferedWriter writer = FileHandler.getBufferedWriter(output);
		writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		writer.write("<!DOCTYPE gnomedoc SYSTEM \""+DTD+"\">\n");
		writer.write("<gnomedoc>\n");
		writer.write(doc.getHeading()+"\n");
		writer.write("<body>\n");
		writer.write(INDENT+"<section>\n");

		int pid=1,sid=1,neid=1,hid=1;
		int psnt=0;
		
		String indent = INDENT+INDENT;
		for (int i=0;i<sentList.size();i++){
			if(i==psnt&&paraCnt.size()>0){
				if(i!=0){
					indent=indent.substring(INDENT.length());
					writer.write(indent+"</p>\n");
				}
				
				psnt+=paraCnt.get(pid-1);
				writer.write(indent+"<p id=\"p"+(pid++)+"\">\n");
				indent+=INDENT;
				
			}
			
			
			writer.write(indent+"<s id=\"s"+(sid++)+"\">\n");
			indent+=INDENT;
			SentenceData sent = sentList.get(i);
			
			
			StringBuilder sb = new StringBuilder();
			int tsize = sent.tokSize();
			boolean[] heads = new boolean[tsize];
			boolean[][] ismarks = new boolean[tsize][tsize];
			int[][] marks = new int[tsize][tsize];
			for(int m=0;m<marks.length;m++)
				Arrays.fill(marks[m], -1);
			
			for(MarkableData mk:sent.getMarkables()){
				int h=mk.getHead();
				if(h>=0)
					heads[h]=true;
				marks[mk.getStart()][mk.getEnd()]=h;
				ismarks[mk.getStart()][mk.getEnd()]=true;
			}
			
			for(int n=1;n<tsize;n++){
				for(int m=marks[n].length-1;m>0;m--)
					if(ismarks[n][m]){
						sb.append(indent+"<ne "
								+ "id=\"ne"+(neid++)+"\" "
								+ NE_REL+"=\""+nullHandler(sent.getLabelById(marks[n][m]))+"\" >\n");
						indent+=INDENT;
					}
				if(heads[n]){	
					sb.append(indent+"<nphead id=\"AAh"+(hid++)+"\">\n");
					indent+=INDENT;
				}
				sb.append(indent+"<W "
						+ W_POS+"=\""+nullHandler(sent.getPosById(n))+"\" "
						+ W_HEAD+"=\""+nullHandler(sent.getHeadById(n))+"\" "
						+ W_REL+"=\""+nullHandler(sent.getLabelById(n))+"\" >");
				sb.append(sent.getXmlTokenById(n));
				sb.append("</W>\n");
				if(heads[n]){
					indent=indent.substring(INDENT.length());
					sb.append(indent+"</nphead>\n");
				}
				for(int m=marks[n].length-1;m>0;m--)
					if(ismarks[m][n]){
						indent=indent.substring(INDENT.length());
						sb.append(indent+"</ne>\n");
					}
				
			}
			writer.write(sb.toString());
			indent=indent.substring(INDENT.length());
			writer.write(indent+"</s>\n");
			
		}
		
		indent=indent.substring(INDENT.length());
		writer.write(indent+"</p>");
		writer.newLine();
		
		writer.write(INDENT+"</section>\n");
		writer.write("</body>\n");
		writer.write("</gnomedoc>\n");
		FileHandler.closeWriter(writer);
		
	}

	

}
