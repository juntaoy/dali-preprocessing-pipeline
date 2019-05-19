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
public class ARRAU extends IFormat  {

	private static ARRAU singleton = new ARRAU();
	private static final String MMAX = "mmax";
	private static final String WORDS = "words";
	private static final String POS = "pos";
	private static final String SENTENCES = "sentence";
	private static final String MARKABLES = "coref";
	
	
	private static final String BASEDIR = "Basedata";
	private static final String MARKDIR = "markables";
	
	private static final String SPAN_SPLITOR = "\\.\\.";
	private static final int WORD_ = "word_".length();
	private static final int MARKABLE_ = "markable_".length();
	
	public static int cross_err = 0, coref_err = 0, type_err=0, ner_err=0;
	
	private ARRAU(){
		FORMATSUFFIX = getMMAXSuffix(MMAX);
		NULLSTR="";
	}
	public static IFormat getInstance() {
		return singleton;
	}
	
	
	
	@Override
	public DocumentData read(File in) throws Exception {
		DocumentData doc = new DocumentData(in);
		
		String[] words = getWordArray(in.getParent(),doc.getFilename());
		String[] pos = getPOSArray(in.getParent(),doc.getFilename());
		if(words.length!=pos.length){
			throw new Exception("Bed document, pos level not at the same lenght as words, len_pos:"+pos.length+", len_word:"+words.length);
		}
		int[][] sents = getSentenceArray(in.getParent(),doc.getFilename());
		int[][] marks = getMarkArray(in.getParent(),doc.getFilename(),pos);
		int[][] w2s = new int[words.length][2]; //sentid, index
		
		for(int s=0;s<sents.length;s++){
			int ind=1;
			for(int w=sents[s][0]; w<=sents[s][1];w++){
				w2s[w][0]=s;
				w2s[w][1]=ind++;
			}
		}
		
		for(int s=0;s<sents.length;s++){
			SentenceData sent = new SentenceData();
			for(int w=sents[s][0]; w<=sents[s][1];w++){
				sent.addTokens(words[w]);
				sent.addPos(pos[w]);
			}
			doc.addSentenceData(sent);
		}
		
		List<SentenceData> sentList = doc.getSentList();
		
		for(int i=0;i<marks.length;i++){
			if(w2s[marks[i][0]][0]!=w2s[marks[i][1]][0]){
				System.err.println("Error, markable cross sentence boundary word_"+marks[i][0]+"..word_"+marks[i][1]+", remove from dataset ");
				cross_err++;
			}else
				sentList.get(w2s[marks[i][0]][0]).addMarkables(
						w2s[marks[i][_mstart]][1], 
						w2s[marks[i][_mend]][1],
						marks[i][_mhead]<0?-1:w2s[marks[i][_mhead]][1],
						marks[i][_mref],
						marks[i][_mset],
						marks[i][_mheadend]<0?-1:w2s[marks[i][_mheadend]][1],
						marks[i][_mid],
						marks[i][_ner],
						marks[i][_lastnn]<0?-1:w2s[marks[i][_lastnn]][1]);
		}
		
		return doc;
	}
	
	private static int _mstart=0, _mend=1,_mref=2, _mset=3, _mhead=4, _mheadend=5, _mid=6,_ner=7,_lastnn=8;
	private int[][] getMarkArray(String indir, String filename,String[] pos) throws Exception{
		Document xml = getMMAXReader(indir,filename, MARKABLES);
		NodeList mList = xml.getElementsByTagName("markable");
		int[][] marks = new int[mList.getLength()][9]; //start, end, referring, setid, head, head_end,mid,lastnn

		for(int i=0;i<mList.getLength();i++){
			Element m = (Element)mList.item(i);
			String spanstr = m.getAttribute("span");
			if(spanstr.indexOf(",")>0)
				spanstr = spanstr.substring(spanstr.lastIndexOf(",")+1);
			String[] span = spanstr.split(SPAN_SPLITOR);
			marks[i][_mstart] = Integer.valueOf(span[0].substring(WORD_));//exclude word_
			marks[i][_mend] = span.length>1?Integer.valueOf(span[1].substring(WORD_)):marks[i][0];//in case sentence only contain 1 token
			int last_nn = -1;
			for(int n=marks[i][_mend];n>=marks[i][_mstart];n--){
				if(pos[n].startsWith("nn")){
					last_nn = n;
					break;
				}
			}
			marks[i][_lastnn] = last_nn;
			String ref_str = m.getAttribute("reference");
			int ref = getARRAUMentionReferringIdByStr(ref_str);
			
			if(ref<0 && m.hasAttribute("non_ref_type")){
				ref_str = m.getAttribute("non_ref_type");
				ref = getARRAUMentionReferringIdByStr(ref_str);
			}
			if(ref<0){
				System.err.println("Error reference type for markable word_"+marks[i][0]+"..word_"+marks[i][1]+", type:"+ref_str+", set to "+getARRAUMentionReferringStrById(0));
				ref = 0;
				type_err++;
			}
			marks[i][_mref]=ref ;
			
			
			int ner = getARRAUMentionTypeIdByStr(m.getAttribute("category"));
			if(ner<0){
				//System.err.println("Error mention type for markable word_"+marks[i][0]+"..word_"+marks[i][1]+", type:"+m.getAttribute("category")+", set to "+getARRAUMentionTypeStrById(0));
				ner = 0;
				ner_err++;
			}
			marks[i][_ner]=ner ;
			
			if(m.getAttribute("coref_set").startsWith("set_"))
				marks[i][_mset] = Integer.valueOf(m.getAttribute("coref_set").substring(4)); // remove set_
			else{
				System.err.println("Error coref_set for markable word_"+marks[i][0]+"..word_"+marks[i][1]+", type:"+getARRAUMentionReferringStrById(marks[i][2])+", set set_id to 0.");
				marks[i][_mset] = 0;
				coref_err++;
			}
			
			
			
			
			
			String hstr = m.hasAttribute("min_ids")?m.getAttribute("min_ids"):"";
			if(hstr.equals("")||!hstr.startsWith("word_")){
				marks[i][_mhead] = -1;
				marks[i][_mheadend] = -1;
			}else{
				String[] tmps = hstr.split(SPAN_SPLITOR);
				marks[i][_mhead] = Integer.valueOf(tmps[0].substring(WORD_));
				marks[i][_mheadend] = tmps.length>1?Integer.valueOf(tmps[1].substring(WORD_)):-1;
			}
			
			String idstr = m.hasAttribute("id")?m.getAttribute("id"):"";
			if(idstr.equals("")){
				throw new Exception("Bad document, markable don't have an id! markable word_"+marks[i][0]+"..word_"+marks[i][1]);
			}else{
				marks[i][_mid] = Integer.valueOf(idstr.substring(MARKABLE_));
			}
		}
		
		
		return marks;
	}
	
	private String[] getPOSArray(String indir,String filename) throws Exception{
		Document xml = getMMAXReader(indir,filename, POS);
		NodeList pList = xml.getElementsByTagName("markable");
		String[] pos = new String[pList.getLength()+1];
		pos[0]=null;//as the word id start with 1;

		for(int i=0;i<pList.getLength();i++){
			Element p = (Element)pList.item(i);
			pos[i+1] = p.getAttribute("tag");
			if(pos[i+1].equals("``"))
				pos[i+1] = "\"";
		}
		
		
		return pos;
		
	}
	private String[] getWordArray(String indir,String filename) throws Exception{
		Document xml = getMMAXReader(indir,filename, WORDS);
		NodeList wList = xml.getElementsByTagName("word");
		String[] words = new String[wList.getLength()+1];
		words[0]=null;//as the word id start with 1;

		for(int i=0;i<wList.getLength();i++){
			Element w = (Element)wList.item(i);
			words[i+1] = w.getTextContent();
		}
		
		
		return words;
		
	}
	
	private int[][] getSentenceArray(String indir, String filename)throws Exception{
		Document xml = getMMAXReader(indir,filename, SENTENCES);
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
		throw new Exception("ARRAU corpus is only used as input for generate IOB2 format, it can't used as an output format!");
	}

	
	
	
	private String getMMAXSuffix(String level){
		if(level.equals(MMAX))
			return ".mmax";
		if(level.equals(WORDS))
			return "_words.xml";
		else
			return "_"+level+"_level.xml";

	}
	
	private Document getMMAXReader(String indir, String filename,  String level) throws Exception{
		String input = indir;
		if(level.equals(MMAX))
			input+=FileHandler.FILE_SEPA+filename+getMMAXSuffix(level);
		else if(level.equals(WORDS))
			input+=FileHandler.FILE_SEPA+BASEDIR+FileHandler.FILE_SEPA+filename+getMMAXSuffix(level);
		else
			input+=FileHandler.FILE_SEPA+MARKDIR+FileHandler.FILE_SEPA+filename+getMMAXSuffix(level);
			
		return getXmlDocument(new File(input));
	}
	

}
