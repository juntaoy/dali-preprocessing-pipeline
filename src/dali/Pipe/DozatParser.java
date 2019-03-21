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
 * Juntao Yu, 26/Jul/2017
 *
 */
public class DozatParser implements IParser {
	private String configFile;
	private String savedir;
	private Options op;
	private String startFile;
    public DozatParser(Options op) {
    	this.savedir = op.getParserModel();
    	configFile = savedir+"/config.cfg";
    	this.op = op;
    	startFile = op.getStartFile();
    }
    
    
    public void parse(){
    	generateConfigFile(op.getConllPath());
    	 System.out.println("Parsing the sentences...");

         String conversionPath = "python "+startFile+" --save_dir " + savedir + " --test";

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
	 	    
	 	    File parsed = new File(op.getDozatParsedPath());
	 	    File conll = new File(op.getConllPath());
 	    	conll.delete();
	 	    
	 	    parsed.renameTo(conll);
	 	    
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
    
    
    
    private void generateConfigFile(String input){
    	BufferedWriter writer = null;
    	BufferedReader reader = null;
		try {
			reader = FileHandler.getBufferedReader(configFile);
			StringBuilder sb = new StringBuilder();
			while(true){
				String line = reader.readLine();
				if(line==null) break;
				if(!line.startsWith("test_file"))
					sb.append(line+"\n");
				else
					sb.append("test_file = "+input+"\n");
			}
			
			reader.close();
						
			writer = FileHandler.getBufferedWriter(configFile);
			writer.write(sb.toString());
    		FileHandler.closeWriter(writer);
		}catch (Exception e) {
			e.printStackTrace();
		}
    }
    
}
