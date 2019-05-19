package dali.file;

import java.io.BufferedReader;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import dali.util.FileHandler;
/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public abstract class IFile {
	protected BufferedReader reader;
	protected String filename;
	protected boolean isHead; 
	
	public static final String htTitle = "<title>";
	public static final String htAuthor = "<author>";
	public static final String htCatStmt = "<categoryStmt>";
	public static final String htDiffStmt= "<difficultyStmt>";
	public static final String htPublisher = "<publisher>";
	public static final String htUrl = "<url>";
	public static final String htAva = "<availability>";
	public static final String htDate = "<date>";
	public static final String htRevisionDesc="<revisionDesc>";
	
	public static final String DALI_FICTION = "DALI-FICTION";
	public static final String DALI_LEARN="DALI-LEARN";
	public static final String DALI_STUDENT_REPORTS="DALI-STUDENT-REPORTS";
	public static final String DALI_WIKI="DALI-WIKI";
	
	public IFile(File file) throws Exception{
		this(file,false);
	}
	
	
	public IFile(File file,boolean noHead) throws Exception{
		reader = FileHandler.getBufferedReader(file);
		filename = file.getName();
		isHead = !noHead;
	}
	
	public String getNext(HashMap<String,String> map) throws Exception{
		if(isHead){
			isHead=false;
			return getHeading(map);
		}else
			return nextPara();
	}
	
	public String getNext() throws Exception {
		if(isHead){
			isHead=false;
			return getHeading();
		}else
			return nextPara();
	}
	
	protected String getHeading(HashMap<String,String> map) throws Exception{
		return getHeading();
	}
	protected abstract String getHeading() throws Exception ;
	
	protected abstract String nextPara() throws Exception ;
		
	protected String getDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMMMM yyyy");
		Date date = new Date();
		return sdf.format(date);
	}
	
	
	protected String getChangeDesc(){
		String change ="" 
				+ "<change>"
				  + "<date>"+getDate()+"</date>"
				  + "<respStmt>"
				    + "<name>Juntao Yu</name>"
				    + "<resp>Automatic Processing</resp>"
				  + "</respStmt>"
				  + "<TEIitem>Sentence split, tokenization, pos tagging, mention extraction, named entity recoganation and mention haed findding</TEIitem>"
				+ "</change>";
		
		return change;
	}
	
	
	/**
	 * Taken from Udo Kruschwitz's CharReplace.java
	 * For ANAWIKI Project
	 * @param str
	 * @return
	 */
	protected String ASCIIFilter(String str){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<str.length();i++){
			char c = str.charAt(i);
			
			// Just output ASCII characters
	        if (((c >= ' ') && (c <= '~')) || (c=='\t') || (c=='\n') || (c=='\r')) {
	          sb.append(c); }
			// Else normalise (some of) the characters (turn them into corresponding ASCII characters)
			else if ((c >= '\u00C0') && (c <= '\u00C6')) {
				sb.append('A');}
			else if (c == '\u00C7') {
				sb.append('C');}
			else if ((c >= '\u00C8') && (c <= '\u00CB')) {
				sb.append('E');}
			else if ((c >= '\u00CB') && (c <= '\u00CF')) {
				sb.append('I');}
			else if (c == '\u00D0') {
				sb.append('D');}
			else if (c == '\u00D1') {
				sb.append('N');}
			else if (((c >= '\u00D2') && (c <= '\u00D6')) ||(c == '\u00D8')) {
				sb.append('O');}
			else if ((c >= '\u00D9') && (c <= '\u00DC')) {
				sb.append('U');}
			else if (c == '\u00DD') {
				sb.append('Y');}
			else if (c == '\u00DF') {
				sb.append('s');
				sb.append('s');}
			  
			else if ((c >= '\u00E0') && (c <= '\u00E6')) {
				sb.append('a');}
			else if (c == '\u00E7') {
				sb.append('c');}
			else if ((c >= '\u00E8') && (c <= '\u00EB')) {
				sb.append('e');}
			else if ((c >= '\u00EB') && (c <= '\u00EF')) {
				sb.append('i');}
			else if (c == '\u00F0') {
				sb.append('d');}
			else if (c == '\u00F1') {
				sb.append('n');}
			else if (((c >= '\u00F2') && (c <= '\u00F6')) ||(c == '\u00F8')) {
				sb.append('o');}
			else if ((c >= '\u00F9') && (c <= '\u00FC')) {
				sb.append('u');}
			else if ((c == '\u00FD') || (c == '\u00FD')) {
				sb.append('y');}
			else if((c=='\u0022') || (c=='\u201C') ||(c=='\u201D')){
				sb.append('\"');}
			else if((c=='\'')||(c=='\u0060') || (c=='\u00B4')||(c=='\u2018')||(c=='\u2019')){
				sb.append('\'');}
			// All other characters: replace by space:
			else {
				sb.append(' ');}
			
			
		}
		return sb.toString();
		
	}
	

}
