package dali.format;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.List;

import dali.util.FileHandler;
import dali.data.DocumentData;
import dali.data.SentenceData;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class Dali  extends IFormat {

	private static Dali singleton = new Dali();
	private static final String SUFFIX = ".dali";

	private Dali(){
		FORMATSUFFIX = SUFFIX;
	}
	public static IFormat getInstance() {
		return singleton;
	}

	@Override
	public DocumentData read(File in) throws Exception {
		DataInputStream dis = FileHandler.getDataInputStream(in);
		DocumentData doc = new DocumentData(in);
		doc.setFilename(dis.readUTF());
		doc.setHeading(dis.readUTF(), false);
		int size = dis.readInt();
		for(int i=0;i<size;i++)
			doc.addParagraph(dis.readInt());
		size = dis.readInt();
		for(int i=0;i<size;i++)
			doc.addSentenceData(new SentenceData(dis));
		return doc;
	}
	

	@Override
	public void write(String out, DocumentData doc) throws Exception {
		String output = out+FileHandler.FILE_SEPA+doc.getFilename()+SUFFIX;
		List<Integer> paraCnt = doc.getParaCnt();
		List<SentenceData> sentList = doc.getSentList();

		DataOutputStream dos = FileHandler.getDataOutputStream(output);
		dos.writeUTF(doc.getFilename());
		dos.writeUTF(doc.getHeading());
		dos.writeInt(paraCnt.size());
		for(int p:paraCnt)
			dos.writeInt(p);
		dos.writeInt(sentList.size());
		for(SentenceData sent:sentList)
			sent.writeData(dos);
		FileHandler.closeWriter(dos);
	}

}
