package dali.format;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dali.data.DocumentData;
import dali.data.MarkableData;
import dali.data.SentenceData;
import dali.util.FileHandler;

public class Twoline  extends IFormat{
	
	private static Twoline singleton = new Twoline();
	private static final String SUFFIX = ".dali2line";

	private Twoline(){
		FORMATSUFFIX = SUFFIX;
	}
	public static IFormat getInstance() {
		return singleton;
	}

	@Override
	public DocumentData read(File in) throws Exception {
		throw new Exception("Dali2line format is not supproted as a input format yet!");
	}
	

	@Override
	public void write(String out, DocumentData doc) throws Exception {
		int err=0;
		String output = out+FileHandler.FILE_SEPA+doc.getFilename()+SUFFIX;
		List<SentenceData> sentList = doc.getSentList();
		BufferedWriter writer = FileHandler.getBufferedWriter(output);
		writer.write("#begin document nw/"+doc.getFilename()+"\n");
		for(SentenceData sent:sentList){
			for(int i=1;i<sent.tokSize();i++){
				if(i!=1)
					writer.write(' ');
				writer.write(sent.getTokenById(i));
			}
			writer.newLine();
			ArrayList<MarkableData> marks = sent.getListMarkables();
			for(int i=0;i<marks.size();i++){
				if(i!=0)
					writer.write(' ');
				//writer.write((marks.get(i).getStart()-1)+","+marks.get(i).getEnd()+","+marks.get(i).getSetId()+","+getARRAUMentionReferringStrById(marks.get(i).getCategory())+",1.0");
						//+","+getARRAUMentionTypeStrById(marks.get(i).getCategory()));//start inclusive, end exclusive
				//if(marks.get(i).getHeadEnd()==-1)
				//	err++;
				writer.write((marks.get(i).getStart()-1)+","+marks.get(i).getEnd()+","+getARRAUMentionTypeStrById(marks.get(i).getNer())+","+(marks.get(i).getMostLikelyHead()-1));
				//+","+getARRAUMentionTypeStrById(marks.get(i).getCategory()));//start inclusive, end exclusive
			}
			writer.newLine();
			writer.newLine();
		}
		if(err>0)
			System.err.println("Error no head:"+err);
		
		FileHandler.closeWriter(writer);
	}

}
