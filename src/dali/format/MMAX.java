package dali.format;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import dali.util.FileHandler;
import dali.data.DocumentData;
import dali.data.MarkableData;
import dali.data.SentenceData;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class MMAX extends IFormat  {

	private static MMAX singleton = new MMAX();
	private static final String MMAX = "mmax";
	private static final String HEADER = "header";
	private static final String WORDS = "words";
	private static final String SENTENCES = "sentence";
	private static final String POS = "pos";
	private static final String PARA = "layout";
	private static final String PARATYPE = "paragraph";
	private static final String DEP = "psrse";
	private static final String MARKABLES = "markable";
	
	private static final String SPAN_SPLITOR = "\\.\\.";
	private static final int WORD_ = 5;
	
	private MMAX(){
		FORMATSUFFIX = getMMAXSuffix(MMAX);
		NULLSTR="";
	}
	public static IFormat getInstance() {
		return singleton;
	}
	
	
	
	@Override
	public DocumentData read(File in) throws Exception {
		DocumentData doc = new DocumentData(in);
		String input = in.getParent()+FileHandler.FILE_SEPA+doc.getFilename();
		//get heading
		doc.setHeading(getXmlHeading(new File(input+getMMAXSuffix(HEADER))));
		
		String[] words = getWordArray(input);
		String[] pos = getPosArray(input);
		String[] parses = getParseArray(input);
		int[][] sents = getSentenceArray(input);
		int[][] paras = getParaArray(input);
		int[][] marks = getMarkArray(input);
		int[][] w2s = new int[words.length][2]; //sentid, index
		
		for(int s=0;s<sents.length;s++){
			int ind=1;
			for(int w=sents[s][0]; w<=sents[s][1];w++){
				w2s[w][0]=s;
				w2s[w][1]=ind++;
			}
		}
		
		for(int i=0;i<paras.length;i++)
			doc.addParagraph(w2s[paras[i][1]][0]-w2s[paras[i][0]][0]+1);// end sent_id - start sent_id +1
		
		for(int s=0;s<sents.length;s++){
			SentenceData sent = new SentenceData();
			for(int w=sents[s][0]; w<=sents[s][1];w++){
				sent.addTokens(words[w]);
				sent.addPos(pos[w]);
			}
			String[] edges = parses[s].split(" ");
			for(int e=0;e<edges.length;e++){
				String[] tmp = edges[e].substring(1, edges[e].length()-1).split(",");
				sent.addHead(Integer.valueOf(tmp[0]));
				sent.addLabel(tmp[1]);
			}
			doc.addSentenceData(sent);
		}
		
		List<SentenceData> sentList = doc.getSentList();
		
		for(int i=0;i<marks.length;i++)
			sentList.get(w2s[marks[i][0]][0]).addMarkables(w2s[marks[i][0]][1], w2s[marks[i][1]][1], w2s[marks[i][2]][1]);
		
		
		return doc;
	}
	
	
	private int[][] getMarkArray(String input) throws Exception{
		Document xml = getMMAXReader(input, MARKABLES);
		NodeList mList = xml.getElementsByTagName("markable");
		int[][] marks = new int[mList.getLength()][3]; //start, end, head

		for(int i=0;i<mList.getLength();i++){
			Element m = (Element)mList.item(i);
			String[] span = m.getAttribute("span").split(SPAN_SPLITOR);
			marks[i][0] = Integer.valueOf(span[0].substring(WORD_));//exclude word_
			marks[i][1] = span.length>1?Integer.valueOf(span[1].substring(WORD_)):marks[i][0];//in case sentence only contain 1 token
			
			marks[i][2] = isNullStr(m.getAttribute("head_ids"))?-1:Integer.valueOf(m.getAttribute("head_ids").substring(WORD_));
			
		}
		
		
		return marks;
	}
	
	private int[][] getParaArray(String input)throws Exception{
		Document xml = getMMAXReader(input, PARA);
		NodeList pList = xml.getElementsByTagName("markable");
		int[][] paras = new int[pList.getLength()][2];

		for(int i=0;i<pList.getLength();i++){
			Element p = (Element)pList.item(i);
			String[] span = p.getAttribute("span").split(SPAN_SPLITOR);
			paras[i][0] = Integer.valueOf(span[0].substring(WORD_));//exclude word_
			paras[i][1] = span.length>1?Integer.valueOf(span[1].substring(WORD_)):paras[i][0];//in case sentence only contain 1 token 
		}
		
		
		return paras;
	}
	
	private String[] getParseArray(String input) throws Exception{
		Document xml = getMMAXReader(input, DEP);
		NodeList depList = xml.getElementsByTagName("markable");
		String[] parses = new String[depList.getLength()];

		for(int i=0;i<depList.getLength();i++){
			Element par = (Element)depList.item(i);
			parses[i] = par.getAttribute("tag");
		}
		
		
		return parses;
		
	}
	
	private String[] getPosArray(String input) throws Exception{
		Document xml = getMMAXReader(input, POS);
		NodeList posList = xml.getElementsByTagName("markable");
		String[] pos = new String[posList.getLength()+1];
		pos[0]=null;//as the word id start with 1;

		for(int i=0;i<posList.getLength();i++){
			Element p = (Element)posList.item(i);
			pos[i+1] = p.getAttribute("tag").toUpperCase();
		}
		
		
		return pos;
		
	}
	
	private String[] getWordArray(String input) throws Exception{
		Document xml = getMMAXReader(input, WORDS);
		NodeList wList = xml.getElementsByTagName("word");
		String[] words = new String[wList.getLength()+1];
		words[0]=null;//as the word id start with 1;

		for(int i=0;i<wList.getLength();i++){
			Element w = (Element)wList.item(i);
			words[i+1] = w.getTextContent();
		}
		
		
		return words;
		
	}
	
	private int[][] getSentenceArray(String input)throws Exception{
		Document xml = getMMAXReader(input, SENTENCES);
		NodeList sList = xml.getElementsByTagName("markable");
		int[][] sents = new int[sList.getLength()][2];

		for(int i=0;i<sList.getLength();i++){
			Element s = (Element)sList.item(i);
			String[] span = s.getAttribute("span").split(SPAN_SPLITOR);
			sents[i][0] = Integer.valueOf(span[0].substring(WORD_));//exclude word_
			sents[i][1] = span.length>1?Integer.valueOf(span[1].substring(WORD_)):sents[i][0];//in case sentence only contain 1 token 
		}
		
		
		return sents;
	}
	
	
	
	@Override
	public void write(String out, DocumentData doc) throws Exception {
		String output = out+FileHandler.FILE_SEPA+doc.getFilename(out);
		List<Integer> paraCnt = doc.getParaCnt();
		List<SentenceData> sentList = doc.getSentList();
		
		//write mmaxfile
		BufferedWriter mmaxwriter = getMMAXWriter(output,MMAX);
		mmaxwriter.write(getMMAXHead(MMAX,doc.getFilename()));
		closeMMAXWriter(MMAX,mmaxwriter);
		
		//write header
		BufferedWriter headerwriter = getMMAXWriter(output,HEADER);
		headerwriter.write(getMMAXHead(HEADER));
		headerwriter.write(doc.getHeading());
		
		closeMMAXWriter(HEADER,headerwriter);
		
		
		//write words, pos, sentence, dep parse and markables
		int[][] words = new int[sentList.size()][];
		BufferedWriter wordwriter = getMMAXWriter(output,WORDS);
		BufferedWriter poswriter = getMMAXWriter(output,POS);
		BufferedWriter sentwriter = getMMAXWriter(output,SENTENCES);
		BufferedWriter depwriter = getMMAXWriter(output,DEP);
		BufferedWriter markwriter = getMMAXWriter(output,MARKABLES);

		int wid=1,pid=0,mid=0;
		
		wordwriter.write(getMMAXHead(WORDS));
		
		poswriter.write(getMMAXHead(POS));
		
		sentwriter.write(getMMAXHead(SENTENCES));
		
		depwriter.write(getMMAXHead(DEP));
		
		markwriter.write(getMMAXHead(MARKABLES));
		
		for(int i=0;i<sentList.size();i++){
			SentenceData sent = sentList.get(i);
			words[i]=new int[sent.tokSize()];
			
			StringBuilder dtree = new StringBuilder();
			
			//words[i][0] is not used as the token is <root>
			for(int j=1;j<sent.tokSize();j++){
				words[i][j]=wid++;
				if(sent.getHeadById(j)>=0)
					dtree.append(" ("+nullHandler(sent.getHeadById(j))+","+nullHandler(sent.getLabelById(j))+")");
				
				wordwriter.write("<word id=\"word_"+words[i][j]+"\">"+sent.getXmlTokenById(j)+"</word>\n");
				
				poswriter.write("<markable id=\"markable_"+(pid++)+"\" "
						+ "span=\"word_"+words[i][j]+"\" "
						+ "tag=\""+nullHandler(sent.getPosById(j)).toLowerCase()+"\" "
						+ "mmax_level=\"pos\" />\n");
			}
			
			
			depwriter.write("<markable id=\"markable_"+i+"\" "
					+ "span=\"word_"+words[i][1] 
					+(words[i].length>2?"..word_"+words[i][words[i].length-1]:"")
					+ "\" tag=\""+dtree.toString().trim()+"\" "
					+ "mmax_level=\"parse\" />\n");
			
			sentwriter.write("<markable id=\"markable_"+i+"\" span=\"word_"+words[i][1]
					+(words[i].length>2?"..word_"+words[i][words[i].length-1]:"")
					+ "\" orderid=\""+i+"\" mmax_level=\"sentence\" />\n");
			
			for(MarkableData mark:sent.getListMarkables()){
				markwriter.write("<markable id=\"markable_"+(mid++)+"\" "
						+ "span=\"word_" + words[i][mark.getStart()] 
						+ (mark.getEnd()>mark.getStart()?"..word_"+words[i][mark.getEnd()]:"")
						+ "\" sentenceid=\""+i+"\" "
						+ "min_ids=\"word_"+words[i][mark.getStart()]+"..word_"+words[i][mark.getEnd()]+"\" "
						+ "head_ids=\"word_"+(mark.getHead()>=0?words[i][mark.getHead()]:NULLSTR)+"\" "
						+ "gram_fnc=\""+nullHandler(sent.getLabelById(mark.getHead()))+"\" "
						+ "head_words=\""+nullHandler(sent.getXmlTokenById(mark.getHead()))+"\" "
						+ "lemmata=\""+sent.getXmlTokenByMarkable(mark).toLowerCase()+"\" "
						+ "pos=\""+sent.getPosByMarkable(mark).toLowerCase()+"\" mmax_level=\"markable\" />\n");
			}
			
		}
		
		
				
		closeMMAXWriter(WORDS,wordwriter);
		closeMMAXWriter(POS,poswriter);
		closeMMAXWriter(SENTENCES,sentwriter);
		closeMMAXWriter(DEP,depwriter);
		closeMMAXWriter(MARKABLES,markwriter);
		
		//write paragraph
		BufferedWriter parawriter = getMMAXWriter(output,PARA);

		parawriter.write(getMMAXHead(PARA));
		
		int scnt = -1;
		for(int i=0;i<paraCnt.size();i++){
			int start = scnt+1;
			int end = scnt+paraCnt.get(i);
			parawriter.write("<markable id=\"markable_"+i+"\" "
					+ "span=\"word_" + words[start][1] + "..word_"+words[end][words[end].length-1]
					+ "\" mmax_level=\""+PARA+"\" "
					+ "type=\""+PARATYPE+"\" />\n");
			scnt+=paraCnt.get(i);
		}
		
		
		
		closeMMAXWriter(PARA,parawriter);
				
		
	}

	private String getMMAXHead(String level){
		return getMMAXHead(level, "");
	}
	
	private String getMMAXHead(String level,String filename){
		if(level.equals(MMAX))
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<mmax_project>\n"
					+ "<words>"+filename+getMMAXSuffix(WORDS)+"</words>\n"
					+ "<keyactions></keyactions>\n"
					+ "<gestures></gestures></mmax_project>\n";
		if(level.equals(HEADER))
			return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
					+ "<!DOCTYPE header SYSTEM \"arrau_header.dtd\">\n";
		if(level.equals(WORDS))
			return "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n"
					+ "<!DOCTYPE words SYSTEM \"words.dtd\">\n"
					+ "<words>\n";
		else
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<!DOCTYPE markables SYSTEM \"markables.dtd\">\n"
				+ "<markables xmlns=\"www.eml.org/NameSpaces/"+level+"\">\n";
	}
	
	private String getMMAXTail(String level){
		if(level.equals(MMAX)||level.equals(HEADER))
			return "";
		if(level.equals(WORDS))
			return "</words>";
		else
			return "</markables>";
	}
	
	private String getMMAXSuffix(String level){
		if(level.equals(MMAX))
			return ".mmax";
		if(level.equals(HEADER))
			return ".header";
		if(level.equals(WORDS))
			return "_words.xml";
		else
			return "_"+level+"_level.xml";

	}
	
	private void closeMMAXWriter(String level, BufferedWriter writer) throws Exception{
		writer.write(getMMAXTail(level));
		FileHandler.closeWriter(writer);
	}
	
	private BufferedWriter getMMAXWriter(String output, String level) throws Exception{
		return FileHandler.getBufferedWriter(output+getMMAXSuffix(level));
	}
	
	private Document getMMAXReader(String input, String level) throws Exception{
		return getXmlDocument(new File(input+getMMAXSuffix(level)));
	}
	

}
