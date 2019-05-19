package dali.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dali.util.FileHandler;
import dali.util.Options;

/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class DocumentData {
	private String heading;
	private String textContent;
	private List<SentenceData> sentList;
	private String filename;
	private String fileType;
	private String daliType;
	private List<Integer> paraCnt;
	
	private static final String INDENT = Options.INDENT;

	public DocumentData(File file){
		this(file,false);
	}
	
	public DocumentData(File file,boolean dalifile){
		heading = "<header>\n</header>";
		sentList =  new ArrayList<SentenceData>();
		filename=extractFilename(file.getName());
		if(dalifile){
			fileType = file.getParentFile().getName();
			daliType = file.getParentFile().getParentFile().getName();
		}
		paraCnt = new ArrayList<Integer>();
	}
	

	private String extractFilename(String name){
		name = name.replaceAll("-header|-masxml|-sgf", "");
		if(name.lastIndexOf(".")>=0)
				name = name.substring(0, name.lastIndexOf("."));
		return name;
	}
	
	public String getHeading(){
		return heading;
	}
	
	public String getTextContent(){
		return textContent;
	}
	
	public void setHeading(String heading, boolean formatting){
		if(formatting==true)
			setHeading(heading);
		else
			this.heading=heading;
	}
	
	
	
	public void setHeading(String heading){
		StringBuilder sb = new StringBuilder();
		String indent=INDENT;
		sb.append(heading.charAt(0));
		for(int i=1;i<heading.length()-1;i++){
			char cd1=heading.charAt(i-1);
			char c = heading.charAt(i);
			char cp1 = heading.charAt(i+1);
			if(c=='<'&&cp1!='/'){
				sb.append("\n"+indent);
				indent+=INDENT;
			}else if(c=='<'&&cp1=='/'){
				indent=indent.substring(INDENT.length());
				if(cd1=='>')
					sb.append("\n"+indent);
				
			}
			
			sb.append(c);
		}
		sb.append(heading.charAt(heading.length()-1));
		this.heading = sb.toString();
		
	}
	
	public void setTextContent(String content){
		this.textContent = content;
	}
	
	public void addParagraph(int cnt){
		this.paraCnt.add(cnt);
	}
	
	public void addSentenceData(SentenceData sent){
		sentList.add(sent);
	}
	
	public List<Integer> getParaCnt(){
		return paraCnt;
	}
	
	public String getFilename(){
		return filename;
	}
	
	public String getDaliType(){
		return daliType;
	}
	
	public String getFileType(){
		return fileType;
	}
	
	public String getFilename(String out){
		if(daliType!=null){
			out += FileHandler.FILE_SEPA+daliType+FileHandler.FILE_SEPA+fileType+FileHandler.FILE_SEPA;
			File dirs = new File(out);
			dirs.mkdirs();
		}
		return daliType+FileHandler.FILE_SEPA+fileType+FileHandler.FILE_SEPA+filename;
	}
	
	public void setFilename(String filename){
		this.filename=filename;
	}
	
	public List<SentenceData> getSentList(){
		return sentList;
	}
	
	
}
