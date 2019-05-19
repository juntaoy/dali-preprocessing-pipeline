package dali.format;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;

import dali.util.FileHandler;
import dali.data.DocumentData;
import dali.data.SentenceData;
import net.sf.saxon.Transform;



/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class SGF  extends IFormat {

	private static SGF singleton = new SGF();
	private static final String SUFFIX_MASXML = "-masxml.xml";
	private static final String SUFFIX_TEXT = "-noheader.txt";
	private static final String SUFFIX_H_TEXT = "-header.txt";
	private static final String SUFFIX = "-sgf.xml";
	private static String xslPath;
	
	private SGF(){
		FORMATSUFFIX = SUFFIX;
	}
	public static IFormat getInstance(String xslPath) {
		SGF.xslPath = xslPath;
		return singleton;
	}

	@Override
	public DocumentData read(File in) throws Exception {
		throw new Exception("SGF format is not supproted as a input format yet!");
	}

	
	@Override
	public void write(String out, DocumentData doc) throws Exception {
		out = new File(out).getAbsolutePath();
		String output = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUFFIX;
		String masxml = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUFFIX_MASXML;
		String text = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUFFIX_TEXT;
		String htext = out+FileHandler.FILE_SEPA+doc.getFilename(out)+SUFFIX_H_TEXT;
		IFormat masio = MASXML_pd.getInstance();
		masio.write(out, doc);
		
		List<Integer> paraCnt = doc.getParaCnt();
		List<SentenceData> sentList = doc.getSentList();

		
		//write the plain text
		BufferedWriter textwriter = FileHandler.getBufferedWriter(text);
		BufferedWriter htextwriter = FileHandler.getBufferedWriter(htext);
		htextwriter.write(doc.getHeading()+"\n");

		int psnt=0;
		int pid=0;
		boolean newpara = true;
		for (int i=0;i<sentList.size();i++){
			if(i==psnt){
				psnt+=paraCnt.get(pid);
				newpara=true;
				if(i!=0){
					textwriter.newLine();
					htextwriter.newLine();
				}
			}
			
			if(!newpara){
				textwriter.write(" ");
				htextwriter.write(" ");
			}
			String sentstr = sentList.get(i).getSentenceStr();
			textwriter.write(sentstr);
			htextwriter.write(sentstr);
			
			newpara=false;
		}
		FileHandler.closeWriter(textwriter);
		FileHandler.closeWriter(htextwriter);
		
		//converted to the sgf format
		
		String[] args = new String[]{"-o",output,masxml,xslPath,"primary-data="+text};
		Transform.main(args);
		
		
	}

}
