package dali.util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class FileHandler {
	public static final String FILE_SEPA = System.getProperty("file.separator");
	
	private FileHandler(){
		
	}
	
	public static List<File> getAllFile(File root){
		List<File> flist = new ArrayList<File>();
		
		if(!root.isHidden()){
			if(root.isFile()){
				flist.add(root);
			}else if(root.isDirectory()){
				File[] children = root.listFiles();
				for(File ch:children)
					flist.addAll(getAllFile(ch));
			}
		}
		return flist;
	}
	
	
	public static DataInputStream getDataInputStream(File input) throws Exception{
		return new DataInputStream(new FileInputStream(input));
	}
	
	public static DataInputStream getDataInputStream(String input) throws Exception{
		return new DataInputStream(new FileInputStream(input));
	}
	
	public static DataOutputStream getDataOutputStream(File output) throws Exception{
		return new DataOutputStream(new FileOutputStream(output));
	}
	
	public static DataOutputStream getDataOutputStream(String output) throws Exception{
		return new DataOutputStream(new FileOutputStream(output));
	}
	
	public static BufferedReader getBufferedReader(File input) throws Exception{
		return new BufferedReader(new InputStreamReader(new FileInputStream(input),"UTF-8"),32768);
	}
	
	public static BufferedReader getBufferedReader(String input) throws Exception{
		return new BufferedReader(new InputStreamReader(new FileInputStream(input),"UTF-8"),32768);
	}
	
	public static BufferedWriter getBufferedWriter(File output) throws Exception{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output),"UTF-8"),32768);
	}
	
	public static BufferedWriter getBufferedWriter(String output) throws Exception{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output),"UTF-8"),32768);
	}
    
    public static void closeWriter(DataOutputStream dos)throws Exception{
		if(dos!=null){
			dos.flush();
			dos.close();
		}
    }
    
    
	public static void closeWriter(BufferedWriter writer)throws Exception{
		if(writer!=null){
			writer.flush();
			writer.close();
		}
	}
	
}
