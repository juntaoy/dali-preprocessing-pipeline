package dali.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import dali.data.DocumentData;
import dali.data.MarkableData;
import dali.data.SentenceData;
import dali.util.FileHandler;


/**
 * DALI Project
 * 
 * Juntao Yu, 30/Aug/2017
 *
 */
public class IOB2 extends IFormat {
	private static IOB2 singleton = new IOB2();
	private static final String SUFFIX = ".iob2";
	private static final String SUBSUFFIX = ".iob2nest";
	private static int  macnt=0,necnt=0,excnt=0;
	
	private IOB2(){
		FORMATSUFFIX = SUFFIX;
		NULLSTR="-";
	}
	public static IFormat getInstance() {
		return singleton;
	}
	
	
	/**
	 * For Evaluator only, not suggested to use as an input format
	 */
	@Override
	public DocumentData read(File in) throws Exception {
		DocumentData doc = new DocumentData(in);
		BufferedReader reader = FileHandler.getBufferedReader(in);
		BufferedReader nested = null;
		String inpath = in.getAbsolutePath();
		File nestin = new File (inpath.substring(0,inpath.length()-5)+SUBSUFFIX);
		if(nestin.exists())
			nested = FileHandler.getBufferedReader(nestin);
		int lcnt=0;
		int scnt=0;
		int start=-1;
		int nstart = -1;
		SentenceData sent = null;
		while(true){
			String line = reader.readLine();
			String nline = null;
			if(nested!=null)
				nline = nested.readLine();
			if(line==null) {
				if(sent!=null){
					doc.addSentenceData(sent);
					sent=null;
				}
				break;
			}
			line = line.trim();
			lcnt++;
			if(line.length()==0){
				if(sent!=null){
					doc.addSentenceData(sent);
					sent=null;
					scnt=0;
				}else
					System.out.println("Skip empty line in line:"+lcnt);
			}else if(line.startsWith("#")){
				System.out.println("Skip comments in line:"+lcnt+"; "+line);
			}else{
				if(sent==null)
					sent = new SentenceData();
				scnt++;
				
				String tmp[] = line.split("\t");
				sent.addTokens(tmp[0]);
				String mark = tmp[tmp.length-1];// last column is the markables;
				if(mark.equals("O")){
					if(start>=0)
						sent.addMarkables(start, scnt-1);
					start=-1;
				}else if(mark.startsWith("I-")||mark.startsWith("B-")){
					if(start<0)
						start = scnt;//support both IOB1 and IOB2
				}else{
					throw new Exception("Please input only IOB or IOB2 scheme (main): "+lcnt+": "+line);
				}
				
				if(nline!=null){
					String ntmp[] = nline.split("\t");
					String nmark = ntmp[ntmp.length-1];
					if(nmark.equals("O")){
						if(nstart>=0)
							sent.addMarkables(start, scnt-1);
						nstart=-1;
					}else if(nmark.startsWith("I-")||nmark.startsWith("B-")){
						if(nstart<0)
							nstart = scnt;
					}else{
						throw new Exception("Please input only IOB or IOB2 scheme (nested): "+lcnt+": "+nline);
					}
				}
				
			}
			
		}
		
		return doc;
	}
	
	
	
	@Override
	public void write(String out, DocumentData doc) throws Exception {
		String output = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUFFIX;
		String nestout = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUBSUFFIX;
		List<SentenceData> sentList = doc.getSentList();		
		String filename = doc.getFilename();
		
		BufferedWriter writer = FileHandler.getBufferedWriter(output);
		BufferedWriter nested = FileHandler.getBufferedWriter(nestout);
		
		int mid=1;
		//int macnt=0,necnt=0,excnt=0;
		
		for (int k=0;k<sentList.size();k++){
			SentenceData sent = sentList.get(k);
			
			int[][] marks = new int[sent.tokSize()][sent.tokSize()];

			for(int j=0;j<marks.length;j++)
				Arrays.fill(marks[j], -1);
			
			HashSet<MarkableData>[] tmpmarks = getSplitNestedMarkable(sent.getMarkables());
			HashSet<MarkableData>[] ntmpmarks = getSplitNestedMarkable(tmpmarks[1]);
			macnt+=tmpmarks[0].size();
			necnt+=ntmpmarks[0].size();
			excnt+=ntmpmarks[1].size();
			
			String[] maiobs = getIOB2Label(tmpmarks[0],sent.tokSize());
			String[] neiobs = getIOB2Label(ntmpmarks[0],sent.tokSize());
			
			for(int i=1;i<sent.tokSize();i++){
				writer.write(sent.getTokenById(i)+"\t"+maiobs[i]);
				writer.newLine();
				
				nested.write(sent.getTokenById(i)+"\t"+neiobs[i]);
				nested.newLine();
			}
			writer.newLine();
			nested.newLine();
			
		}
		
		System.out.println(doc.getFilename()+", unnested:"+macnt+", nested:"+necnt+", skiped:"+excnt+", skiprate:"+(excnt*100.0/(macnt+necnt+excnt)));
		
		FileHandler.closeWriter(writer);
		FileHandler.closeWriter(nested);
			
	}
	
	/**
	 * 
	 * @param unnested the markabledata need to be unnested first
	 * @param len
	 * @return
	 */
	private String[] getIOB2Label(HashSet<MarkableData> unnested, int len){
		String[] iob = new String[len];
		for(int i=0;i<len;i++)
			iob[i]="O";
		
		for(MarkableData m: unnested){
			for(int i=m.getStart();i<=m.getEnd();i++){
				if(i==m.getStart())
					iob[i]="B-OTH";
				else
					iob[i]="I-OTH";
			}
		}
		return iob;
	}
	
	private HashSet<MarkableData>[] getSplitNestedMarkable(HashSet<MarkableData> marks){
		HashSet<MarkableData>[] splited = new HashSet[2];
		splited[0] = new HashSet<MarkableData>();//for unnested
		splited[1] = new HashSet<MarkableData>();//for nested
		
		for(MarkableData m1:marks){
			boolean isnested = false;
			for(MarkableData m2:marks){
				if(m1!=m2&&m2.getStart()<=m1.getStart()&&m2.getEnd()>=m1.getEnd()){
					isnested = true;
					break;
				}
			}
			splited[isnested?1:0].add(m1);
		}
		
		return splited;
	}

}
