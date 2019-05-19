package dali.main;




import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
	private IAnotator annotator;
	private ParaToSentData p2sd;
	private List<IFormat> outs;
	
	public Pipeline(Options op){
		this.op = op;
		init();
	}
	
	private void init(){
		
		try {
			if(op.getAnnotator()==Options.ANNOTATOR_MATE){
				annotator = new MateParser(op);
			}else if(op.getAnnotator()==Options.ANNOTATOR_DOZAT){
				annotator = new DozatParser(op);
			}else{
				annotator = new DaliTagger(op);
			}
			p2sd = new ParaToSentData(op.getTagModel());
			
			outs = new ArrayList<IFormat>();
			String outFormat = op.getOutFormat();

			if(outFormat.contains(Options.FORMAT_DALI))
				outs.add(Dali.getInstance());
			if(outFormat.contains(Options.FORMAT_MASXML))
				outs.add(MASXML.getInstance());
			if(outFormat.contains(Options.FORMAT_MASXML_PD)&&!outFormat.contains(Options.FORMAT_SGF))
				outs.add(MASXML_pd.getInstance());
			if(outFormat.contains(Options.FORMAT_CONLL))
				outs.add(CONLL12.getInstance());
			if(outFormat.contains(Options.FORMAT_MMAX))
				outs.add(MMAX.getInstance());
			if(outFormat.contains(Options.FORMAT_SGF))
				outs.add(SGF.getInstance(op.getXslPath()));
			if(outFormat.contains(Options.FORMAT_IOB2))
				outs.add(IOB2.getInstance());
			if(outFormat.contains(Options.FORMAT_2LINE))
				outs.add(Twoline.getInstance());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void processing() throws Exception{
		List<File> fileList = FileHandler.getAllFile(new File(op.getInPath()));
		HashMap<String, HashMap<String,String>> htMaps = new HashMap<String,HashMap<String,String>>();
		for(File file:fileList){
			if(file.getName().endsWith("_header_template.xml")){
				HashMap<String,String> tmap = new HashMap<String,String>();
				String filename = file.getName().replace("_header_template.xml", "");
				BufferedReader reader = FileHandler.getBufferedReader(file);
				while(true){
					String line = reader.readLine();
					if(line==null)break;
					line = line.trim();
					if(line.length()==0) continue;
					int len = line.length();
					if(line.startsWith(IFile.htTitle)){
						if(line.length()>(IFile.htTitle.length()*2+1))
							tmap.put(IFile.htTitle, line.substring(IFile.htTitle.length(), len-IFile.htTitle.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htAuthor)){
						if(line.length()>(IFile.htAuthor.length()*2+1))
							tmap.put(IFile.htAuthor, line.substring(IFile.htAuthor.length(), len-IFile.htAuthor.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htCatStmt)){
						if(line.length()>(IFile.htCatStmt.length()*2+1))
							tmap.put(IFile.htCatStmt, line.substring(IFile.htCatStmt.length(), len-IFile.htCatStmt.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htDiffStmt)){
						if(line.length()>(IFile.htDiffStmt.length()*2+1))
							tmap.put(IFile.htDiffStmt, line.substring(IFile.htDiffStmt.length(), len-IFile.htDiffStmt.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htPublisher)){
						if(line.length()>(IFile.htPublisher.length()*2+1))
							tmap.put(IFile.htPublisher, line.substring(IFile.htPublisher.length(), len-IFile.htPublisher.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htUrl)){
						if(line.length()>(IFile.htUrl.length()*2+1))
							tmap.put(IFile.htUrl, line.substring(IFile.htUrl.length(), len-IFile.htUrl.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htAva)){
						if(line.length()>(IFile.htAva.length()*2+1))
							tmap.put(IFile.htAva, line.substring(IFile.htAva.length(), len-IFile.htAva.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htDate)){
						if(line.length()>(IFile.htDate.length()*2+1))
							tmap.put(IFile.htDate, line.substring(IFile.htDate.length(), len-IFile.htDate.length()-1));
						else
							continue;
					}else if(line.startsWith(IFile.htRevisionDesc)){
						String redesc = "";
						String endReDesc = "</"+IFile.htRevisionDesc.substring(1);
						while(true){
							line = reader.readLine();
							if(line==null) break;
							line = line.trim();
							if(line.length()==0)continue;
							if(line.endsWith(endReDesc)){
								if(line.equals(endReDesc))
									break;
								else
									redesc+=line.substring(0, line.length()-endReDesc.length());
							}
							redesc+=line;
						}
						
						if(redesc.length()>0)
							tmap.put(IFile.htRevisionDesc, redesc);
						
					} 
					
					
				}//end while
				
				if(tmap.size()>0){
					htMaps.put(filename, tmap);
				}

			}//end if
		}
		
		
		List<DocumentData> docList = new ArrayList<DocumentData>();
		for(File file:fileList){
			if(!(file.getName().endsWith(".txt")||file.getName().endsWith(".htm")||file.getName().endsWith(".html")))
				continue;
			DocumentData doc = new DocumentData(file,op.getDocType()==Options.DOC_DALI);
			IFile fileReader = op.getDocType()==Options.DOC_DALI?new TextFile(file):(op.getDocType()==Options.DOC_GUTENBERG?new Gutenberg(file):new Wikipedia(file));
			HashMap<String,String> tmap = null;
			if(doc.getDaliType()!=null){
				String htkey = doc.getDaliType().equals(IFile.DALI_WIKI)?doc.getFileType().toLowerCase():doc.getFilename();
				for(String key : htMaps.keySet()){
					if(htkey.startsWith(key)){
						tmap = htMaps.get(key);
					}
				}
			}
			String heading = tmap==null?fileReader.getNext():fileReader.getNext(tmap);
			if(heading == null)
				throw(new Exception("This file does not contains a vaild head: "+file.getAbsolutePath()));
			doc.setHeading(heading);
			String textContent="";
			String para = "";
			while(true){
				para = fileReader.getNext();
				if(para==null) break;
				textContent += textContent.length()>0?"\n":"";
				List<SentenceData> sList = p2sd.getSentenceDataListFromParagraph(para, textContent.length());
				if(sList.size()>0){
					doc.addParagraph(sList.size());
					for(SentenceData sent:sList)
						doc.addSentenceData(sent);
				}
				textContent += para;
			}
			doc.setTextContent(textContent);
			docList.add(doc);
		}
		
		BufferedWriter writer = FileHandler.getBufferedWriter(op.getTmpFilePath());
		for(DocumentData doc:docList)
			for(SentenceData sent: doc.getSentList()){
				if(op.getAnnotator()==Options.ANNOTATOR_DALI){
					for(int i=1;i<sent.tokSize();i++){
						if(i!=1)
							writer.write(' ');
						writer.write(sent.getTokenById(i));
					}
					writer.newLine();
					writer.newLine();
					writer.newLine();					
				}else
					sent.writeCoNLL0609Data(writer, op.getAnnotator()==Options.ANNOTATOR_MATE);//mate -c09 dozat -c06
			}
		FileHandler.closeWriter(writer);
		
		annotator.anotate();
		
		if(op.getAnnotator()==Options.ANNOTATOR_MATE){
			CONLLReader09 preader = new CONLLReader09(op.getTmpFilePath());
			for(DocumentData doc:docList)
				for(SentenceData sent: doc.getSentList())
					sent.addMarkables(preader.getNext());
				
		}else if(op.getAnnotator()==Options.ANNOTATOR_DOZAT){
			CONLLReader06 preader = new CONLLReader06(op.getTmpFilePath());
			for(DocumentData doc:docList)
				for(SentenceData sent: doc.getSentList())
					sent.addMarkables(CoNLL06GoldToPred(preader.getNext()));
				
		}else{
			//dali tagger
			BufferedReader reader = FileHandler.getBufferedReader(op.getTmpFilePath());
			for(DocumentData doc:docList)
				for(SentenceData sent:doc.getSentList()){
					String[] tokens = reader.readLine().split(" ");
					if(sent.tokSize()-1 != tokens.length)
						throw new Exception("Sentence length miss match:"+(sent.tokSize()-1)+" vs "+tokens.length);
					String[] head_scores = reader.readLine().split(" ");
					if(head_scores.length!=tokens.length)
						throw new Exception("Head socres' lenght miss match:"+tokens.length+" vs "+head_scores.length);
					for(String h:head_scores)
						sent.addHeadScores(Double.valueOf(h));
					
					String[] marks = reader.readLine().split(" ");
					for(String m:marks){
						if(m.length()==0)
							continue;
						String[] t = m.split(",");
						sent.addMarkables(Integer.parseInt(t[0])+1, Integer.parseInt(t[1]), Integer.parseInt(t[3])+1,t[2]);
					}
					
					reader.readLine();
				}
			
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
			System.out.println("-g|-w					The type of the input document, -g: Gutenberg; -w: Wikipedia; Default:"+Options.DOCTYPE_NAME[op.getDocType()]);
			System.out.println("-dalitagger|-mate|-dozat				The parser you would like to use for parsing, -mate: Bohnet and Nivre (2012) -dozat: Dozat and Manning (2016); Default:"+Options.ANNOTATOR_NAME[op.getAnnotator()]);
			System.out.println("-startFile				If you use the Dozat parser, you could specify the location of the main file; Default:"+op.getStartFile());
			System.out.println("-input <dir>				The directory that contains the documents to be processed; Default:"+op.getInPath());
			System.out.println("-output <dir>				The directory to output the processed documents; Defalut:"+op.getOutPath());
			System.out.println("-tmodel <file>				The location of the model for part-of-speech; Defalut:"+op.getTagModel());
			System.out.println("-pmodel <dir> 				The location of the model (-save_dir) of Dozat parser; Defalut:"+op.getPmodel());
			System.out.println("-xsl <file> 				The location of the xsl file required by the sgf converter; Defalut:"+op.getXslPath());
			System.out.println("-dali -masxml -masxmlpd -sgf -conll -mmax	-iob2	The output format of the documents; Default:"+op.getOutFormat());
		}else {
		
			Options op = new Options(args);
			
			System.out.println("Dali pipeline configurations:");
			System.out.println("Tagger model:"+op.getTagModel());
			System.out.println("Annotator used:"+Options.ANNOTATOR_NAME[op.getAnnotator()]);
			if(op.getAnnotator()!=Options.ANNOTATOR_MATE)
				System.out.println("Start File:"+op.getStartFile());
			System.out.println("Parser model:"+op.getPmodel());

			if(op.getOutFormat().contains("sfg"))
				System.out.println("XSL file:"+op.getXslPath());
			
			System.out.println("Input dir:"+op.getInPath());
			System.out.println("Output dir:"+op.getOutPath());
			System.out.println("Conll file:"+op.getTmpFilePath());
			System.out.println("Document type:"+Options.DOCTYPE_NAME[op.getDocType()]);
			System.out.println("Output Format:"+op.getOutFormat());
			
			Pipeline  pipe = new Pipeline(op);
			pipe.processing();
		}
	}
}
