package dali.util;

import java.io.File;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class Options {
	private String tagModel = "models/english-bidirectional-distsim.tagger";
	private String pmodel = "DaliTagger/models/model-";//"models/chen-indomain-mate-Baseline-rescore-model-joint";//"DozatParser/P27/saves/t1/";
	private String startFile = "DaliTagger/test_pipe.py";//"DozatParser/P27/network.py";
	private int doctype = DOC_DALI;
	private int annotator=ANNOTATOR_DALI;
	private String inPath = "input";
	private String outPath = "output";
	private String tmpFileName = "gutenberg.tmp";
	
	private String inFormat = "dali";
	private String outFormat = "-masxml";
	private String xslPath = "models/MASXML2SGF.xsl";
	private boolean isAnalyse = false;
	
	public static final String INDENT = " ";

	public static final String[] ANNOTATOR_NAME = {"Dali Tagger","Mate Parser","Dozat Parser"};
	public static final String[] DOCTYPE_NAME = {"Guntenberg","Wikipedia","DaliDoc"};
	public static final int ANNOTATOR_DALI = 0;
	public static final int ANNOTATOR_MATE = 1;
	public static final int ANNOTATOR_DOZAT = 2;
	public static final int DOC_DALI = 2;
	public static final int DOC_GUTENBERG = 0;
	public static final int DOC_WIKIPEDIA = 1;
	public static final String FORMAT_DALI = "dali";
	public static final String FORMAT_MASXML = "masxml";
	public static final String FORMAT_MASXML_PD = "masxmlpd";
	public static final String FORMAT_SGF = "sgf";
	public static final String FORMAT_CONLL = "conll";
	public static final String FORMAT_MMAX = "mmax";
	public static final String FORMAT_IOB2 = "iob2";
	public static final String FORMAT_ARRAU = "arrau";
	public static final String FORMAT_2LINE = "2line";
	
	
	public Options()throws Exception{
		this(new String[] {});
	}
			
	public Options(String args[]) throws Exception{
		this(args, false);
	}
	
	public Options(String args[], boolean isAnalyse) throws Exception{
		boolean format = false;
		
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-g"))
				doctype=DOC_GUTENBERG;
			else if(args[i].equals("-w"))
				doctype = DOC_WIKIPEDIA;
			else if(args[i].equals("-d"))
				doctype = DOC_DALI;
			else if(args[i].equals("-tmodel"))
				tagModel = args[i+1];
			else if(args[i].equals("-pmodel"))
				pmodel = args[i+1];
			else if(args[i].equals("-input"))
				inPath = args[i+1];
			else if(args[i].equals("-output"))
				outPath = args[i+1];
			else if(args[i].matches("-dali|-masxml|-masxmlpd|-sgf|-conll|-mmax|-iob2|-2line")){
				if(!format){
					outFormat="";
					format=true;
				}
				outFormat += args[i];
			}else if(args[i].equals("-xsl"))
				xslPath = args[i+1];
			else if(args[i].equals("-dozat"))
				annotator=ANNOTATOR_DOZAT;
			else if(args[i].equals("-mate"))
				annotator=ANNOTATOR_MATE;
			else if(args[i].equals("-dalitagger"))
				annotator=ANNOTATOR_DALI;
			else if(args[i].equals("-inFormat"))
				inFormat=args[i+1];
			else if(args[i].equals("-startFile"))
				startFile=args[i+1];
				
		}
		
		this.isAnalyse = isAnalyse;
		
		if((!isAnalyse)&&inFormat.equals(FORMAT_ARRAU)&&!outFormat.matches("-iob2|-2line"))
			throw new Exception("ARRAU format can only be converted iob2 or dali2line format, change outFormat to -iob2");
			
			
		
		if(doctype==DOC_GUTENBERG)
			tmpFileName = "gutenberg.tmp";
		else if(doctype==DOC_WIKIPEDIA)
			tmpFileName = "wikipedia.tmp";
		else
			tmpFileName = "dalidoc.tmp";

		
	}
	
	public String getStartFile(){
		return startFile;
	}
	
	public String getInFormat(){
		return inFormat;
	}
	public String getOutFormat(){
		return outFormat;
	}

	public String getXslPath(){
		return xslPath;
	}
	public String getDozatParsedPath(){
		return pmodel+"/"+tmpFileName;
	}
	public String getTmpFileName(){
		return tmpFileName;
	}
	public String getTmpFilePath(){
		return outPath+"/"+tmpFileName;
	}
	
	public String getMateParsedPath(){
		return outPath+"/"+tmpFileName+".parsed";
	}
	
	public String getTagModel() {
		return tagModel;
	}

	public String getPmodel() {
		return pmodel;
	}

	public int getDocType() {
		return doctype;
	}
	public int getAnnotator(){
		return annotator;
	}

	public String getInPath() {
		return inPath;
	}

	public String getOutPath() {
		return outPath;
	}

}
