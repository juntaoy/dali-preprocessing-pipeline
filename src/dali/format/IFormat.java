package dali.format;


import java.io.BufferedReader;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import dali.data.DocumentData;
import dali.util.FileHandler;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public abstract class IFormat {
	
	protected String FORMATSUFFIX;
	protected String NULLSTR;
	public abstract DocumentData read(File in) throws Exception;
	
	
	public abstract void write(String out,DocumentData doc) throws Exception;

	protected Document getXmlDocument(File in)throws Exception{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document xml = dBuilder.parse(in);
		xml.getDocumentElement().normalize();
		
		return xml;
	}
	
	
	protected boolean isNullStr(String str){
		if(str.equals(NULLSTR))
			return true;
		return false;
	}
	
	protected String nullHandler(int h){
		if(h<0)
			return NULLSTR;
		return ""+h;
	}
	protected String nullHandler(String str){
		if(str==null)
			return NULLSTR;
		return str;
	}
	
	public boolean isValidFile(File file){
		if(file.getName().endsWith(FORMATSUFFIX))
			return true;
		return false;
	}
	
	//private static final String[] MREFS={"non_referring","new", "old"};
	private static final String[] MREFS={"unknown","expletive","predicate","quantifier","coordination","idiom","new", "old"};
	
	protected String getARRAUMentionReferringStrById(int id){
		if(id<0 || id>= MREFS.length)
			return "ERROR";
		return MREFS[id];
	}
	
	protected int getARRAUMentionReferringIdByStr(String str){
		for(int i=0;i<MREFS.length;i++)
			if(str.equals(MREFS[i]))
				return i;
		return -1;
	}
	
	private static final String[] MTYPES={"unknown","person","animate","concrete",
			"organization","space","time","numerical","plan","abstract"};
	
	public static String getARRAUMentionTypeStrById(int id){
		if(id<=0||id>=MTYPES.length)
			id=0;
		return MTYPES[id];
	}
	
	public static int getARRAUMentionTypeIdByStr(String str){
		for(int i=0;i<MTYPES.length;i++)
			if(str.equals(MTYPES[i]))
				return i;
		
		return -1;
	}
	
	protected String getXmlHeading(File in) throws Exception{
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = FileHandler.getBufferedReader(in);
		boolean inHead =false;
		while(true){
			String line = reader.readLine();
			if(line==null)break;
			line = line.trim();
			if(line.length()==0)
				continue;
			if(!inHead)
				if(line.contains("<header>"))
					inHead=true;
				else
					continue;
			
			if(inHead)
				sb.append(line);
				
			if(line.contains("</header>"))
				return sb.toString();
		}
		
		
		return null;
	}
	
}
