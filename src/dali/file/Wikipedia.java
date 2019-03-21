package dali.file;

import java.io.File;
/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class Wikipedia extends IFile {
	
	private static final String wiki_author = "Wikipedia";
    private static final String wiki_publisher = "Wikipedia";
    private static final String wiki_availability = "All text is available under the terms of the GNU Free Documentation License";
	
	public Wikipedia(File file) throws Exception {
		super(file);
	}
	
	public Wikipedia(File file, boolean noHead) throws Exception {
		super(file,noHead);
	}

	@Override
	protected String getHeading() throws Exception {
		String line = "";
		String title="", server="",pagename="";
		while(true){
			line = reader.readLine();
			if(line==null)break;
			line = line.trim();
			
			if(line.contains("wgServer"))
				server = line.split("\"")[1];
			else if(line.contains("wgPageName"))
				pagename = line.split("\"")[1];
			else if(line.contains("var wgTitle"))
				title = line.split("\"")[1];
			
			if(line.contains("</head>"))
				break;
		}
		
		if(server.length()==0||pagename.length()==0)
			return null;
		return 
			 "<header>"
			+ "<fileDesc>"
			  + "<titleStmt>"
			    + "<title>" + title + "</title>"
			    + "<author>" + wiki_author + "</author>"
			  + "</titleStmt>"
		      + "<publicationStmt>"
		        + "<web>"
		          + "<publisher>" + wiki_publisher + "</publisher>" 
	              + "<url>" + server + "/wiki/" + pagename + "</url>"
	            + "</web>"
	            + "<availability>" + wiki_availability + "</availability>"
	            + "<date>" + getDate() + "</date>"
	          + "</publicationStmt>"
	          + "<sourceDesc>"
	            + "<filename>data\\game\\wikipedia\\" + filename + "</filename>"
	            + "<date>" + getDate() + "</date>" 
	          + "</sourceDesc>"
	        + "</fileDesc>"
	        + "<revisionDesc>" + getChangeDesc() + "</revisionDesc>"
	      + "</header>";
	}

	@Override
	protected String nextPara() throws Exception {
		String line = "";
		while(true){
			line = reader.readLine();
			if(line==null)break;
			line = line.trim();
			line = ASCIIFilter(line);
			
			if(line.matches("<p\\b[^>]*>.*</p>")){
				//isAParagraph
				line = line.replaceAll("(<[^>]*>)\\s*", " ");//replace all<*></>
				line = line.replaceAll("\\[\\s*(edit|citation needed)\\s*\\]", " ");//replace [edit][citation needed]
				line = line.replaceAll("\\[[A-Za-z0-9\\s]*\\]", " ");//replace [number]
				line = line.replaceAll("&#[0-9]*;", " ");//replace &#000;
				line = line.replaceAll("[\\s+]", " ");//replace multiple space
				line = line.trim();
				
				if(line.contains("class=\"toctext\""))//shouldn't have this
					System.err.println(line);
				
				if(line.length()>0)
					return line;
			}
		}
		
		return null;
	}
	
	
	
	
}
