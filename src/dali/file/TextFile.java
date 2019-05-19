package dali.file;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;

import dali.util.FileHandler;
/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class TextFile extends IFile{
	
	private String daliType;
	private String fileType;
	
	public TextFile(File file) throws Exception {
		super(file);
		fileType = file.getParentFile().getName();
		daliType = file.getParentFile().getParentFile().getName();
	}
	
	public TextFile(File file, boolean noHead) throws Exception {
		super(file,noHead);
		fileType = file.getParentFile().getName();
		daliType = file.getParentFile().getParentFile().getName();
	}

	
	

	private static final String wiki_author = "Wikipedia";
    private static final String wiki_publisher = "Wikipedia";
    private static final String wiki_availability = "All text is available under the terms of the GNU Free Documentation License";
	
	private String headdir = "Batch_1_preprocessed_v4_new";
	
	protected String getTmpHeading() throws Exception{
		if(!daliType.equals(DALI_WIKI)){
			String li = reader.readLine();//take out the tiltle
			while(li.trim().length()>0)
				li = reader.readLine();
		}
		BufferedReader tmp = FileHandler.getBufferedReader(headdir+"/"+daliType+"/"+fileType+"/"+filename.substring(0, filename.lastIndexOf("."))+"-masxml.xml");
		String line = "";
		StringBuilder hsb = new StringBuilder();
		boolean head = false;

		while(true){
			line = tmp.readLine();
			if(line==null)break;
			line= line.trim();
			if(line.contains("<header>"))
				head=true;
			if(head)
				hsb.append(line);
			if(line.contains("</header>"))
				break;
		}
		return hsb.toString();
	}
	
	@Override
	protected String getHeading(HashMap<String,String> map) throws Exception {
		if(daliType.equals(DALI_WIKI)){
			String line = "";
			String title="", server="https://en.wikipedia.org",pagename="";
			while(true){
				line = reader.readLine();
				if(line==null)break;
				line = line.trim();
				
				
				if(line.contains("wgPageName")){
					String[] tmp = line.split("\"");
					for (int i=0;i<tmp.length;i++)
						if(tmp[i].equals("wgPageName")){
							pagename = tmp[i+2];
							break;
						}
				}
				if(line.contains("wgTitle")){
					String[] tmp = line.split("\"");
					for (int i=0;i<tmp.length;i++)
						if(tmp[i].equals("wgTitle")){
							title = tmp[i+2];
							break;
						}
				}
				
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
				  +"<categoryStmt>"
				  +(map.containsKey(htCatStmt)?map.get(htCatStmt):fileType.replaceAll("_", ""))
				  +"</categoryStmt>"
				  +"<difficultyStmt>"
				  +(map.containsKey(htDiffStmt)?map.get(htDiffStmt):"E")
				  +"</difficultyStmt>"
			      + "<publicationStmt>"
			        + "<web>"
			          + "<publisher>" + (map.containsKey(htPublisher)?map.get(htPublisher):wiki_publisher) + "</publisher>" 
		              + "<url>" + (map.containsKey(htUrl)?map.get(htUrl):(server + "/wiki/" + pagename)) + "</url>"
		            + "</web>"
		            + "<availability>" + (map.containsKey(htAva)?map.get(htAva):wiki_availability) + "</availability>"
		            + "<date>" + (map.containsKey(htDate)?map.get(htDate):getDate()) + "</date>"
		          + "</publicationStmt>"
		          + "<sourceDesc>"
		            + "<filename>data\\game\\wikipedia\\" + filename + "</filename>"
		            + "<date>" + (map.containsKey(htDate)?map.get(htDate):getDate()) + "</date>" 
		          + "</sourceDesc>"
		        + "</fileDesc>"
		        + "<revisionDesc>" +(map.containsKey(htRevisionDesc)?map.get(htRevisionDesc):"") +getChangeDesc() + "</revisionDesc>"
		      + "</header>";
		      
		}else{
			String title = map.containsKey(htTitle)?map.get(htTitle):reader.readLine().trim();		
			
			String diff = "E";
			if(map.containsKey(htDiffStmt))
				diff = map.get(htDiffStmt);
			else if(daliType.equals(DALI_LEARN))
				diff = filename.substring(0,1);
			else if(daliType.equals(DALI_STUDENT_REPORTS))
				diff = "D";
			else if(fileType.equals("Fairy_Tales"))
				diff = "D";
			
			return "<header>"
					+ "<fileDesc>"
					  + "<titleStmt>"
					    + "<title>" + title + "</title>"
					    + "<author>" + (map.containsKey(htAuthor)?map.get(htAuthor):"unknown") + "</author>"
					  + "</titleStmt>"
					  +"<categoryStmt>"
					  + (map.containsKey(htCatStmt)?map.get(htCatStmt):fileType.replaceAll("_", ""))
					  +"</categoryStmt>"
					  +"<difficultyStmt>"
					  + diff
					  +"</difficultyStmt>"
				      + "<publicationStmt>"
				        + "<web>"
				          + "<publisher>" + (map.containsKey(htPublisher)?map.get(htPublisher):"unknown") + "</publisher>" 
			              + "<url>" +  (map.containsKey(htUrl)?map.get(htUrl):(daliType+ "/"+fileType+"/" + filename)) + "</url>"
			            + "</web>"
			            + "<availability>" + (map.containsKey(htAva)?map.get(htAva):"unknown") + "</availability>"
			            + "<date>" + (map.containsKey(htDate)?map.get(htDate):getDate()) + "</date>"
			          + "</publicationStmt>"
			          + "<sourceDesc>"
			            + "<filename>data\\game\\"+daliType+"\\"+fileType+"\\" + filename + "</filename>"
			            + "<date>" + (map.containsKey(htDate)?map.get(htDate):getDate()) + "</date>" 
			          + "</sourceDesc>"
			        + "</fileDesc>"
			        + "<revisionDesc>" + (map.containsKey(htRevisionDesc)?map.get(htRevisionDesc):"") +getChangeDesc() + "</revisionDesc>"
			      + "</header>";
			      
		}
		//return getTmpHeading();
	}
	
	@Override
	protected String getHeading() throws Exception {
		/*if(daliType.equals(DALI_WIKI)){
			String line = "";
			String title="", server="https://en.wikipedia.org",pagename="";
			while(true){
				line = reader.readLine();
				if(line==null)break;
				line = line.trim();
				
				
				if(line.contains("wgPageName")){
					String[] tmp = line.split("\"");
					for (int i=0;i<tmp.length;i++)
						if(tmp[i].equals("wgPageName")){
							pagename = tmp[i+2];
							break;
						}
				}
				if(line.contains("wgTitle")){
					String[] tmp = line.split("\"");
					for (int i=0;i<tmp.length;i++)
						if(tmp[i].equals("wgTitle")){
							title = tmp[i+2];
							break;
						}
				}
				
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
				  +"<categoryStmt>"
				  +fileType.replaceAll("_", "")
				  +"</categoryStmt>"
				  +"<difficultyStmt>"
				  +"E"
				  +"</difficultyStmt>"
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
		      
		}else{
			String title = reader.readLine().trim();
			
			String diff = "E";
			if(daliType.equals(DALI_LEARN))
				diff = filename.substring(0,1);
			else if(daliType.equals(DALI_STUDENT_REPORTS))
				diff = "D";
			else if(fileType.equals("Fairy_Tales"))
				diff = "D";
			
			return "<header>"
					+ "<fileDesc>"
					  + "<titleStmt>"
					    + "<title>" + title + "</title>"
					    + "<author>" + "unknown" + "</author>"
					  + "</titleStmt>"
					  +"<categoryStmt>"
					  +fileType.replaceAll("_", "")
					  +"</categoryStmt>"
					  +"<difficultyStmt>"
					  +diff
					  +"</difficultyStmt>"
				      + "<publicationStmt>"
				        + "<web>"
				          + "<publisher>" + "unknown" + "</publisher>" 
			              + "<url>" +  daliType+ "/"+fileType+"/" + filename + "</url>"
			            + "</web>"
			            + "<availability>" + "unknown" + "</availability>"
			            + "<date>" + getDate() + "</date>"
			          + "</publicationStmt>"
			          + "<sourceDesc>"
			            + "<filename>data\\game\\"+daliType+"\\"+fileType+"\\" + filename + "</filename>"
			            + "<date>" + getDate() + "</date>" 
			          + "</sourceDesc>"
			        + "</fileDesc>"
			        + "<revisionDesc>" + getChangeDesc() + "</revisionDesc>"
			      + "</header>";
			      
		}*/
		return getTmpHeading();
	}

	@Override
	protected String nextPara() throws Exception {
		if(daliType.equals(DALI_WIKI)){
			String para = "";
			String line = "";
			while(true){
				line = reader.readLine();
				if(line==null)break;
				line = line.trim();
				line = ASCIIFilter(line);
				if(line.matches("<p\\b[^>]*>.*")||line.matches("<h[1-9]>.*"))
					para = line;
				else
					para+=" "+line;
				
				if(para.matches("<p\\b[^>]*>.*</p>")||para.matches("<h[1-9]>.*</h[1-9]>")){
					//isAParagraph
					para = para.replaceAll("(<[^>]*>)\\s*", " ");//replace all<*></>
					para = para.replaceAll("\\[\\s*(edit|citation needed)\\s*\\]", " ");//replace [edit][citation needed]
					para = para.replaceAll("\\[[A-Za-z0-9\\s]*\\]", " ");//replace [number]
					para = para.replaceAll("&#[0-9]*;", " ");//replace &#000;
					para = para.replaceAll("&nbsp;", " ");//replace &nbsp;
					para = para.replaceAll("[\\s+]", " ");//replace multiple space
					para = para.trim();
					
					if(para.contains("class=\"toctext\""))//shouldn't have this
						System.err.println(para);
					
					if(para.length()>0)
						return para;
				}
			}
			
			return null;
		}else{
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

	
	
}
