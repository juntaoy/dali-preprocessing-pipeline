package dali.file;

import java.io.File;
/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class Gutenberg extends IFile{

	public Gutenberg(File file) throws Exception {
		super(file);
	}
	
	public Gutenberg(File file, boolean noHead) throws Exception {
		super(file,noHead);
	}

	@Override
	protected String getHeading() throws Exception {
		String line = "";
		StringBuilder hsb = new StringBuilder();
		while(true){
			line = reader.readLine();
			if(line==null)break;
			line = line.trim();
		
			if(line.length()==0)
				continue;
			if(line.contains("<revisionDesc>")){
				line =	line.replace("<revisionDesc>", "<revisionDesc>"+getChangeDesc());
			}
			hsb.append(line);
			if(line.contains("</header>"))
				break;
		}
		
		if(hsb.length()==0)
			return null;
		return hsb.toString();
	}

	@Override
	protected String nextPara() throws Exception {
		String line = "";
		StringBuilder spara = new StringBuilder();
		while(true){
			line = reader.readLine();
			if(line==null)break;
			line = line.trim();
			line = ASCIIFilter(line);
			
			if(line.length()==0&&spara.length()!=0)
				break;
			
			if(spara.length()>0)
				spara.append(" ");
			spara.append(line);
		}
		
		if(spara.length()==0)
			return null;
		return spara.toString();
	
	}

	
	
}
