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
	private static final String DTD = "dtd/mas_xml_DALI.dtd";
	private static final String W_POS = "Lpos";
	private static final String W_START="start";
	private static final String W_END="end";
	private static final String W_HSCORE="att_score";
	//private static final String W_HEAD = "Lhead";
	//private static final String W_REL = "Lrel";
	//private static final String NE_REL = "AAgf";
	private static final String NE_NER = "nerType";
	
	private HashSet<String> stopMarkableId;
	
	
	
	private MASXML(){
		FORMATSUFFIX = ".xml";
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
		
		String textContent = xml.getElementsByTagName("textualContent").item(0).getTextContent();	
		doc.setTextContent(textContent);
		
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
		String neNer = "";
		int rehead=-1;
		int head=-1;
		for(int i=0;i<children.getLength();i++){
			Node child = children.item(i);
			
			String chName = child.getNodeName();
			if(start>=0){
				if(!stopMarkableId.contains(neId))
					sent.addMarkables(start, sent.tokSize()-1,head,getARRAUMentionTypeIdByStr(neNer));
				start=-1;
				head=-1;
				neId="";
			}
			if(chName.equals("W")){
				Element ech = (Element) child;
				sent.addTokens(child.getTextContent());
				if(!isNullStr(ech.getAttribute(W_POS)))
					sent.addPos(ech.getAttribute(W_POS));
				if(!isNullStr(ech.getAttribute(W_START)))
					sent.addTStart(Integer.valueOf(ech.getAttribute(W_START)));
				if(!isNullStr(ech.getAttribute(W_END)))
					sent.addTEnd(Integer.valueOf(ech.getAttribute(W_END)));
				//if(!isNullStr(ech.getAttribute(W_HEAD)))
				//	sent.addHead(Integer.valueOf(ech.getAttribute(W_HEAD)));
				//if(!isNullStr(ech.getAttribute(W_REL)))
				//	sent.addLabel(ech.getAttribute(W_REL));
			}else if(chName.equals("ne")){
				//the ne will start with next token so no need use size-1;
				start = sent.tokSize();
				neId = ((Element) child).getAttribute("id");
				neNer = ((Element) child).getAttribute(NE_NER);
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
		
		String output = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUFFIX;
		List<Integer> paraCnt = doc.getParaCnt();
		List<SentenceData> sentList = doc.getSentList();
		
		BufferedWriter writer = FileHandler.getBufferedWriter(output);
		writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		writer.write("<!DOCTYPE dalidoc SYSTEM \""+DTD+"\">\n");
		writer.write("<dalidoc>\n");
		writer.write(doc.getHeading()+"\n");
		writer.write("<primaryData start=\"0\" end=\""+doc.getTextContent().length()+"\">\n<textualContent>\n");
		writer.write(SentenceData.convertXMLchars(doc.getTextContent())+"\n");
		writer.write("</textualContent>\n</primaryData>\n");
		
		//writer.write("<body>\n");
		//writer.write(INDENT+"<section>\n");

		int lid=1,nerid=1,neid=1;//,hid=1;
		int psnt=0;
		StringBuilder baselayer = new StringBuilder();
		StringBuilder layoutlayer = new StringBuilder();
		StringBuilder marklayer = new StringBuilder();
		StringBuilder nerlayer=  new StringBuilder();
		baselayer.append("<baseLayer>\n");
		layoutlayer.append("<layoutLayer>\n");
		marklayer.append("<markableLayer>\n");
		nerlayer.append("<nerLayer>\n");
		
		int pid = 0;
		int[][] para_se =null;
		if(paraCnt.size()>0){
			para_se = new int[paraCnt.size()][2];
		}
		int[][] sent_se = new int[sentList.size()][2];
		
		int pretid = 0;
		
		for (int i=0;i<sentList.size();i++){
			if(i==psnt&&paraCnt.size()>0){
				if(i!=0)
					para_se[pid-1][1] = pretid-1;
				
				para_se[pid][0] = pretid;
				psnt+=paraCnt.get(pid++);
			}
			if(i!=0)
				sent_se[i-1][1] = pretid-1;
			sent_se[i][0] = pretid;
			
			SentenceData sent = sentList.get(i);
			
			
			int tsize = sent.tokSize();
			boolean[] heads = new boolean[tsize];
			boolean[][] ismarks = new boolean[tsize][tsize];
			int[][] marks = new int[tsize][tsize];
			int[][] ners = new int[tsize][tsize];
			for(int m=0;m<marks.length;m++){
				Arrays.fill(marks[m], -1);
				Arrays.fill(ners[m], -1);
			}
			
			for(MarkableData mk:sent.getMarkables()){
				int h=mk.getHead();
				int ner = mk.getNer();
				if(h>=0)
					heads[h]=true;
				marks[mk.getStart()][mk.getEnd()]=h;
				ismarks[mk.getStart()][mk.getEnd()]=true;
				ners[mk.getStart()][mk.getEnd()] = ner;
			}
			
			for(int n=1;n<tsize;n++){
				baselayer.append(INDENT+"<W id=\"w"+(pretid+n-1)+"\" "
						+ W_POS+"=\""+nullHandler(sent.getXmlPosByID(n))+"\" "
						+ W_START+"=\""+nullHandler(sent.getTStartById(n))+"\" "
						+ W_END+"=\""+nullHandler(sent.getTEndById(n))+"\" "
						+ W_HSCORE+"=\""+sent.getHeadScoresById(n)+"\" >");
						
						//+ W_HEAD+"=\""+nullHandler(sent.getHeadById(n))+"\" "
						//+ W_REL+"=\""+nullHandler(sent.getLabelById(n))+"\" >");
				baselayer.append(sent.getXmlTokenById(n));
				baselayer.append("</W>\n");
			}
			for(int k=1;k<tsize;k++){
				if(heads[k]){
					marklayer.append(INDENT+"<nh head=\"w"+(pretid+k-1)+"..w"+(pretid+k-1)+"\">\n");
					for(int n=1;n<tsize;n++){
						for(int m=1;m<tsize;m++){
							if(ismarks[n][m]&&marks[n][m]==k)
								marklayer.append(INDENT+INDENT+"<am "
										+ "id=\"am"+(neid++)+"\" "
										+"span=\"w"+(pretid+n-1)+"..w"+(pretid+m-1)+"\" "
										+"player=\"dalipipeline201806\" />\n");
										
						}
					}
					marklayer.append(INDENT+"</nh>\n");
				}
			}
			
			for(int n=1;n<tsize;n++){
				for(int m=1;m<tsize;m++){
					if(ismarks[n][m]){
						nerlayer.append(INDENT+"<nerMarkable id=\"ner"+(nerid++)+"\" "
								+ "start=\"w"+(pretid+n-1)+"\" end=\"w"+(pretid+m-1)+"\" "
										+ NE_NER+"=\""+nullHandler(getARRAUMentionTypeStrById(ners[n][m]))+"\" />\n");
					}
				}
			}
			
			pretid+=tsize-1;
		}
		
		layoutlayer.append(INDENT+"<layMarkable id=\"l"+(lid++)+"\" start=\"w0\" end=\"w"+(pretid-1)+"\" layType=\"section\" />\n");
		if(para_se==null)
			layoutlayer.append(INDENT+"<layMarkable id=\"l"+(lid++)+"\" start=\"w0\" end=\"w"+(pretid-1)+"\" layType=\"p\" />\n");
		else{
			para_se[para_se.length-1][1] = pretid-1;
			for(int i=0;i<para_se.length;i++)
				layoutlayer.append(INDENT+"<layMarkable id=\"l"+(lid++)+"\" start=\"w"+para_se[i][0]+"\" end=\"w"+para_se[i][1]+"\" layType=\"p\" />\n");

		}
		sent_se[sent_se.length-1][1] = pretid-1;
		for(int i=0;i<sent_se.length;i++)
			layoutlayer.append(INDENT+"<layMarkable id=\"l"+(lid++)+"\" start=\"w"+sent_se[i][0]+"\" end=\"w"+sent_se[i][1]+"\" layType=\"s\" />\n");

		baselayer.append("</baseLayer>\n");
		layoutlayer.append("</layoutLayer>\n");
		marklayer.append("</markableLayer>\n");
		nerlayer.append("</nerLayer>\n");
		writer.write(baselayer.toString());
		writer.write(layoutlayer.toString());
		writer.write(marklayer.toString());
		writer.write(nerlayer.toString());
		
		writer.write("</dalidoc>\n");
		FileHandler.closeWriter(writer);
		
	}

	

}
