package dali.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
public class CONLL12 extends IFormat {
	private static CONLL12 singleton = new CONLL12();
	private static final String SUFFIX = ".conll12";
	private static final String PARA = "#<p>";
	
	
	private static final int COLUMN_TOK = 3;
	private static final int COLUMN_POS = 4;
	private static final int COLUMN_DEP = 5;

	
	private CONLL12(){
		FORMATSUFFIX = SUFFIX;
		NULLSTR="-";
	}
	public static IFormat getInstance() {
		return singleton;
	}
	
	
	
	@Override
	public DocumentData read(File in) throws Exception {
		DocumentData doc = new DocumentData(in);
		BufferedReader reader = FileHandler.getBufferedReader(in);
		int pcnt = 0;
		int lcnt=0;
		int scnt=0;
		SentenceData sent = null;
		HashMap<Integer,Integer> currMarks = new HashMap<Integer,Integer>();
		while(true){
			String line = reader.readLine();
			if(line==null) {
				if(sent!=null){
					doc.addSentenceData(sent);
					pcnt++;
					sent=null;
				}
				if(pcnt!=0){
					doc.addParagraph(pcnt);
					pcnt=0;
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
					pcnt++;
				}else
					System.out.println("Skip empty line in line:"+lcnt);
			}else if(line.startsWith("#")){
				if(line.equals(PARA)){
					if(pcnt!=0){
						doc.addParagraph(pcnt);
						pcnt=0;
					}
				}else
					System.out.println("Skip comments in line:"+lcnt+"; "+line);
					
			}else{
				if(sent==null)
					sent = new SentenceData();
				scnt++;
				
				String tmp[] = line.split("\t");
				sent.addTokens(tmp[COLUMN_TOK]);
				if(!isNullStr(tmp[COLUMN_POS]))
					sent.addPos(tmp[COLUMN_POS]);
				if(!isNullStr(tmp[COLUMN_DEP])){
					String[] deps = tmp[COLUMN_DEP].split(" ");
					sent.addHead(Integer.valueOf(deps[0])+1);
					sent.addLabel(deps[1]);
				}
				
				String mark = tmp[tmp.length-1];// last column is the markables;
				if(!isNullStr(mark)){
					boolean LB = false;
					String str_num="";
					for(int c=0;c<mark.length();c++){
						char ch = mark.charAt(c);
						if(ch<='9'&&ch>='0')
							str_num+=ch;
						else{
							if(LB){
								currMarks.put(Integer.valueOf(str_num), scnt);
							}
							if(ch=='('){
								LB=true;
								str_num="";
							}else if(ch==')'){
								int key = Integer.valueOf(str_num);
								sent.addMarkables(currMarks.get(key), scnt);
								currMarks.remove(key);
								LB=false;
								str_num="";
							}else if(ch=='|'){
								str_num="";
								LB=false;
							}
							else
								throw(new Exception("Unexpected items:"+line));
						}
						
					}
					if(LB)
						currMarks.put(Integer.valueOf(str_num), scnt);
				}
				
			}
			
		}
		
		for(SentenceData s:doc.getSentList())
			s.resetMarkableHeadIdByDep();
		
		return doc;
	}
	
	
	
	@Override
	public void write(String out, DocumentData doc) throws Exception {
		String output = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUFFIX;
		List<Integer> paraCnt = doc.getParaCnt();
		List<SentenceData> sentList = doc.getSentList();		
		String filename = doc.getFilename();
		
		BufferedWriter writer = FileHandler.getBufferedWriter(output);
		
		int mid=1;
		int psnt=0,pid=0;
		
		for (int k=0;k<sentList.size();k++){
			if(k==psnt&&paraCnt.size()>0){
				psnt+=paraCnt.get(pid++);
				if(k!=0){
					writer.write(PARA);
					writer.newLine();
				}
			}
			
			SentenceData sent = sentList.get(k);
			
			int[][] marks = new int[sent.tokSize()][sent.tokSize()];

			for(int j=0;j<marks.length;j++)
				Arrays.fill(marks[j], -1);
			
			for(MarkableData mk:sent.getListMarkables()){
				marks[mk.getStart()][mk.getEnd()]=mid++;
			}
			
			
			
			for(int i=1;i<sent.tokSize();i++){
				writer.write(filename+"\t0\t"+(i-1)+"\t"+sent.getPennTokenById(i)+"\t"+nullHandler(sent.getPosById(i))+"\t"+ getParse(sent.getHeadById(i),sent.getLabelById(i))+"\t-\t-\t-\t-\t*\t*\t*\t");
				String ms = "";
				for(int m=marks.length-1;m>=0;m--)
					if(marks[m][i]>=0&&m!=i)
						ms+="|"+marks[m][i]+")";
				
				for(int m=marks[i].length-1;m>=0;m--){
					if(marks[i][m]>=0){
						ms+="|("+marks[i][m];
						if(i==m)
							ms+=")";
					}
				}
				writer.write(ms.length()>0?ms.substring(1):NULLSTR);
				
				writer.newLine();
			}
			writer.newLine();
			
		}
		
		FileHandler.closeWriter(writer);
			
	}
	
	
	private String getParse(int head, String label){
		if(label==null||head==-1)
			return NULLSTR;
		else 
			return (head-1)+" "+label;
	}
	
	
}
