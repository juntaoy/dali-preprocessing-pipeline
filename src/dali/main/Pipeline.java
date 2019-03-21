package dali.main;




import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dali.Pipe.*;
import dali.format.*;
import dali.util.*;
import dali.data.*;
import dali.file.*;
import is2.data.SentenceData09;
import is2.io.CONLLReader06;
import is2.io.CONLLReader09;

/**
 * DALI Project: pre-processing pipeline
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class Pipeline {
	private Options op;	
	private IParser parser;
	private ParaToSentData p2sd;
	private List<IFormat> outs;
	
	public Pipeline(Options op){
		this.op = op;
		init();
	}
	
	private void init(){
		
		try {
			if(op.isMate()){
				parser = new MateParser(op);
			}else{
				parser = new DozatParser(op);
			}
			p2sd = new ParaToSentData(op.getTagModel());
			
			outs = new ArrayList<IFormat>();
			String outFormat = op.getOutFormat();

			if(outFormat.contains(Options.FORMAT_DALI))
				outs.add(Dali.getInstance());
			if(outFormat.contains(Options.FORMAT_MASXML)&&!outFormat.contains(Options.FORMAT_SGF))
				outs.add(MASXML.getInstance());
			if(outFormat.contains(Options.FORMAT_CONLL))
				outs.add(CONLL12.getInstance());
			if(outFormat.contains(Options.FORMAT_MMAX))
				outs.add(MMAX.getInstance());
			if(outFormat.contains(Options.FORMAT_SGF))
				outs.add(SGF.getInstance(op.getXslPath()));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void processing() throws Exception{
		List<File> fileList = FileHandler.getAllFile(new File(op.getInPath()));

			
		List<DocumentData> docList = new ArrayList<DocumentData>();
		for(File file:fileList){
			DocumentData doc = new DocumentData(file);
			IFile fileReader = op.isGutenberg()?new Gutenberg(file):new Wikipedia(file);
			String heading = fileReader.getNext();
			if(heading == null)
				throw(new Exception("This file does not contains a vaild head: "+file.getAbsolutePath()));
			doc.setHeading(heading);
			String para = "";
			while(true){
				para = fileReader.getNext();
				if(para==null) break;
				List<SentenceData> sList = p2sd.getSentenceDataListFromParagraph(para);
				if(sList.size()>0){
					doc.addParagraph(sList.size());
					for(SentenceData sent:sList)
						doc.addSentenceData(sent);
				}
			}
			docList.add(doc);
		}
		
		BufferedWriter writer = FileHandler.getBufferedWriter(op.getConllPath());
		for(DocumentData doc:docList)
			for(SentenceData sent: doc.getSentList())
				sent.writeCoNLL0609Data(writer, op.isMate());//mate -c09 dozat -c06
			
		FileHandler.closeWriter(writer);
		
		parser.parse();
		
		if(op.isMate()){
			CONLLReader09 preader = new CONLLReader09(op.getConllPath());
			for(DocumentData doc:docList)
				for(SentenceData sent: doc.getSentList())
					sent.addMarkables(preader.getNext());
				
		}else{
			CONLLReader06 preader = new CONLLReader06(op.getConllPath());
			for(DocumentData doc:docList)
				for(SentenceData sent: doc.getSentList())
					sent.addMarkables(CoNLL06GoldToPred(preader.getNext()));
				
		}
		
		for(DocumentData doc:docList)
			for(IFormat io:outs)
				io.write(op.getOutPath(), doc);
		
	}
	
	//only for dozat parser
	private SentenceData09 CoNLL06GoldToPred(SentenceData09 ins){
		ins.pheads=ins.heads;
		ins.plabels=ins.labels;
		return ins;
	}
	
	
	
	
	public static void main(String[] args)throws Exception{
		//java -Xmx5g -cp dist/dalipipeline.jar Dali.main.Pipeline -g -input input-g -output output -parser DozatParser/P27/saves/t1 -dtd
		if(args.length>0&&args[0].matches("-h|--help")) {
			Options op = new Options();
			System.out.println("************Welcome to the DALI Pre-processing pipeline*********\n"
					+ "\nThis is the pre-processing pipeline for DALI project, which processes Gutenberg and Wikepedia "
					+ "\ndocuments with a number of tasks to generate masxml and sgf files required by the down stream "
					+ "\napplication. The pipeline processes the documents by the sentence spliter, tokenizer, part-of"
					+ "\n-speech tagger, dependency parser and mention detector.");
			System.out.println("");
			System.out.println("Useage: java -Xmx5g -cp dalipipeline.jar dali.main.Pipeline [Options]");
			System.out.println("");
			System.out.println("--help|-h				Help");
			System.out.println("-g|-w					The type of the input document, -g: Gutenberg; -w: Wikipedia; Default:"+(op.isGutenberg()?"-g":"-w"));
			System.out.println("-mate|-dozat				The parser you would like to use for parsing, -mate: Bohnet and Nivre (2012) -dozat: Dozat and Manning (2016); Default:"+(op.isMate()?"-mate":"-dozat"));
			System.out.println("-startFile				If you use the Dozat parser, you could specify the location of the main file; Default:"+op.getStartFile());
			System.out.println("-input <dir>				The directory that contains the documents to be processed; Default:"+op.getInPath());
			System.out.println("-output <dir>				The directory to output the processed documents; Defalut:"+op.getOutPath());
			System.out.println("-tmodel <file>				The location of the model for part-of-speech; Defalut:"+op.getTagModel());
			System.out.println("-pmodel <dir> 				The location of the model (-save_dir) of Dozat parser; Defalut:"+op.getParserModel());
			System.out.println("-xsl <file> 				The location of the xsl file required by the sgf converter; Defalut:"+op.getXslPath());
			System.out.println("-dali -masxml -sgf -conll -mmax		The output format of the documents; Default:"+op.getOutFormat());
		}else {
		
			Options op = new Options(args);
			
			System.out.println("Dali pipeline configurations:");
			System.out.println("Tagger model:"+op.getTagModel());
			System.out.println("Parser used:"+(op.isMate()?"Mate":"Dozat"));
			if(!op.isMate())
				System.out.println("Start File:"+op.getStartFile());
			System.out.println("Parser model:"+op.getParserModel());

			if(op.getOutFormat().contains("sfg"))
				System.out.println("XSL file:"+op.getXslPath());
			
			System.out.println("Input dir:"+op.getInPath());
			System.out.println("Output dir:"+op.getOutPath());
			System.out.println("Conll file:"+op.getConllPath());
			System.out.println("Document type:"+(op.isGutenberg()?"Gutenberg":"Wikipedia"));
			System.out.println("Output Format:"+op.getOutFormat());
			
			Pipeline  pipe = new Pipeline(op);
			pipe.processing();
		}
	}
}
