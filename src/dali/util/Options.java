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
	private String parserModel = "models/chen-indomain-mate-Baseline-rescore-model-joint";//"DozatParser/P27/saves/t1/";
	private String dozatStartFile = "DozatParser/P27/network.py";
	private boolean isGutenberg = true;
	private boolean isMate=true;
	private String inPath = "input";
	private String outPath = "output";
	private String conllName = "gutenberg.conll";
	
	private String inFormat = "dali";
	private String outFormat = "-dali-masxml-sgf";
	private String xslPath = "models/MASXML2SGF.xsl";
	
	
	public static final String INDENT = " ";

	public static final String FORMAT_DALI = "dali";
	public static final String FORMAT_MASXML = "masxml";
	public static final String FORMAT_SGF = "sgf";
	public static final String FORMAT_CONLL = "conll";
	public static final String FORMAT_MMAX = "mmax";
	
	
	public Options(){
		this(new String[] {});
	}
			
	public Options(String args[]){
		boolean format = false;
		
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-g"))
				isGutenberg=true;
			else if(args[i].equals("-w"))
				isGutenberg = false;
			else if(args[i].equals("-tmodel"))
				tagModel = args[i+1];
			else if(args[i].equals("-pmodel"))
				parserModel = args[i+1];
			else if(args[i].equals("-input"))
				inPath = args[i+1];
			else if(args[i].equals("-output"))
				outPath = args[i+1];
			else if(args[i].matches("-dali|-masxml|-sgf|-conll|-mmax")){
				if(!format){
					outFormat="";
					format=true;
				}
				outFormat += args[i];
			}else if(args[i].equals("-xsl"))
				xslPath = args[i+1];
			else if(args[i].equals("-dozat"))
				isMate=false;
			else if(args[i].equals("-inFormat"))
				inFormat=args[i+1];
			else if(args[i].equals("-startFile"))
				dozatStartFile=args[i+1];
		}
		
		if(isGutenberg)
			conllName = "gutenberg.conll";
		else
			conllName = "wikipedia.conll";

		
	}
	
	public String getStartFile(){
		return dozatStartFile;
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
		return parserModel+"/"+conllName;
	}
	public String getConllName(){
		return conllName;
	}
	public String getConllPath(){
		return outPath+"/"+conllName;
	}
	
	public String getMateParsedPath(){
		return outPath+"/"+conllName+".parsed";
	}
	
	public String getTagModel() {
		return tagModel;
	}

	public String getParserModel() {
		return parserModel;
	}

	public boolean isGutenberg() {
		return isGutenberg;
	}
	public boolean isMate(){
		return isMate;
	}

	public String getInPath() {
		return inPath;
	}

	public String getOutPath() {
		return outPath;
	}

}
