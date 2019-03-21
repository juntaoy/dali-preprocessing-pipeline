package dali.Pipe;

import java.io.File;

import dali.util.Options;
import is2.transitionS2.Parser;

/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class MateParser implements IParser {
	/* Default setting
	 * set th=0.25	# .25 for joint postagging and parsing
	 * set mth=0.1
	 * set b=40	# the size of the beam during parsing
	 * set ts=2	# 2, for 2 pos tags to be input into the parser (1 - for using GS postags)
	 * set ms=1
	 * set hm=0
	 * set ht=4	#4, how large the beam should be for the pos tags
	 */
	private final String[] configs = new String[]{"-test","","-eval","","-out","","-model",""
			,"-tsize","2","-msize","1","-hm","0","-ht","4","-tthreshold","0.25","-mthreshold","0.1","-beam","40"};
	private static final int testInd=1, evalInd=3, outInd=5, modelInd=7;
	public MateParser(Options op){
		configs[testInd] = op.getConllPath();
		configs[evalInd] = op.getConllPath();
		configs[outInd] = op.getMateParsedPath();
		configs[modelInd] = op.getParserModel();
	}
	
	
	
	public void parse(){
		try {
			Parser.main(configs);
			 File parsed = new File(configs[outInd]);
		 	 File conll = new File(configs[testInd]);
	 	     conll.delete();
		 	 parsed.renameTo(conll);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
			
}
