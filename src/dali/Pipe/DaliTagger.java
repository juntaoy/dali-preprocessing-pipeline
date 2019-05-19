package dali.Pipe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import dali.util.Options;
import dali.util.FileHandler;

/**
 * DALI Project
 * 
 * Juntao Yu, 12/Jun/2018
 *
 */
public class DaliTagger implements IAnotator {
	private String model_prefix;
	private Options op;
	private String startFile;
    public DaliTagger(Options op) {
    	this.model_prefix = op.getPmodel();
    	this.op = op;
    	startFile = op.getStartFile();
    }
    
    
    public void anotate(){
    	 System.out.println("Annotating the sentences...");

         String conversionPath = "python "+startFile+
        		 " --mention_model " + model_prefix + "mention"
        		 +" --ner_model " + model_prefix +"ner"
        		 +" --head_model " + model_prefix +"head"
        		 + " --input "+ op.getTmpFilePath()
        		 +" --output "+ op.getTmpFilePath();

         //System.out.println("(1) " + conversionPath);
         Runtime rt = Runtime.getRuntime();
         //System.out.println("(2) " + conversionPath);

         try {

	 	    // UK 26/11/08: avoid problem of hanging process (i.e. waitFor() never comes back)
	 	    //    --> read in all output, otherwise process blocks
	 	    //        (solution taken from here: http://forums.sun.com/thread.jspa?threadID=236394)
	             Process pr = rt.exec(conversionPath);
	 	    
	 	    ////////////////////////////////
	 	    FlushStream error = new FlushStream(pr.getErrorStream());
	 	    FlushStream output= new FlushStream(pr.getInputStream());
	 	    error.start();
	 	    output.start();
	 	    pr.waitFor();
	 	    //System.out.println("after executing RunProcess");
	 	    pr.destroy();
	 	    pr = null;
	 	    System.gc();
	 	    ////////////////////////////////
	
	 	    //System.out.println("(3) " + conversionPath);
	             //pr.waitFor();
	 	    //System.out.println("(4) " + conversionPath);
	 	   
	 	    
         } catch (Exception ex) {
         	ex.printStackTrace();
         }
    	
    	
    }
    
    

// UK 26/11/08: this is part of the solution to the hanging process problem (see above)
// See: http://forums.sun.com/thread.jspa?threadID=236394)

	class FlushStream extends Thread {
	    InputStream is;
	
	    FlushStream (InputStream is) {
	    	this.is = is;
	    }
	
	    public void run() {
			try {
			    InputStreamReader isr = new InputStreamReader(is);
			    BufferedReader br = new BufferedReader(isr);
			    String line=null;
			    while ( (line = br.readLine()) != null) {
			    	System.out.println("BUFFER" + line);
			    }
			}
			catch (IOException ioe) {
			    ioe.printStackTrace();
			}
	    }
	}
    
    
    
   
    
}
