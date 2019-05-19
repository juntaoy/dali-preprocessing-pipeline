package dali.Pipe;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import dali.data.SentenceData;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


/**
 * DALI Project
 * 
 * Juntao Yu, 26/Jul/2017
 *
 */
public class ParaToSentData {
	private MaxentTagger postagger;

	public ParaToSentData(String tagModel) throws Exception{
		postagger = new MaxentTagger(tagModel);
	}
	
	public List<SentenceData> getSentenceDataListFromParagraph(String para,int pre_ind){
		List<SentenceData> sentList = new ArrayList<SentenceData>();
		List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(para)) ;
		for(List<HasWord> sentence:sentences){
			List<TaggedWord> tagged = postagger.tagSentence(sentence);
			SentenceData sent = new SentenceData();
			for(TaggedWord tw:tagged){
				sent.addTokens(tw.word());
				sent.addTStart(pre_ind+tw.beginPosition());
				sent.addTEnd(pre_ind+tw.endPosition());
				sent.addPos(tw.tag());
			}
			//System.out.println(sent);
			sentList.add(sent);
		}
		return sentList;
	}
	
}
