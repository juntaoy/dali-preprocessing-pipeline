# Dali Mention Tagger
This folder contains the neural mention detector introduced in the following papers:

**[Crowdsourcing and Aggregating Nested Markable Annotations](https://aclweb.org/anthology/papers/P/P19/P19-1077/)**  
Chris Madge, Juntao Yu, Jon Chamberlain, Udo Kruschwitz, Silviu Paun and Massimo Poesio
In *Proceedings of the The 57th Annual Meeting of the Association for Computational Linguistics (ACL)*, 2019

**[Anaphora Resolution with the ARRAU Corpus](https://www.aclweb.org/anthology/papers/W/W18/W18-0702/)**  
Massimo Poesio, Yulia Grishina, Varada Kolhatkar, Nafise Moosavi, Ina Roesiger, Adam Roussel, Fabian Simonjetz, Alexandra Uma, Olga Uryupina, Juntao Yu, Heike Zinsmeister  
In *Proceedings of the First Workshop on Computational Models of Reference, Anaphora and Coreference (CRAC)*, 2018

## Requirement
In order to run the system you need to first install Python 2.7, tensorflow 1.8, numpy and cPickle.

## Prepare data
The sytem takes a *two line* format, for each sentence, the first line stores its tokens and the second line stores its annotations (mention, mention types and mention head). A empty line are used to separete different sentences. For example:

```
Hoare Govett is acting as the consortium 's investment bankers .
0,2,organization,1 5,8,organization,6 5,10,abstract,9

The Merksamer chain is currently being offered for sale by First Boston Corp .
0,3,organization,2 8,9,plan,8 10,13,organization,12

```
In which the tokens and mentions are separated by blank space, and inner mention annotations are separated by comma. The mention annotations are in \[start_index, end_index (exclusive), mention_type, mention_head\] format. For sentences that don't contain any mention or you are annotating a raw text simple leave the second line empty. e.g.

```
Hoare Govett is acting as the consortium 's investment bankers .


The Merksamer chain is currently being offered for sale by First Boston Corp .


```

## The three modes
The tagger itself is a pipeline that first predict the mentions, then assign each mention a type and a mention head. We currently support the following mention types: \["unknown","person","animate","concrete","organization","space","time","numerical","plan","abstract"\]. 
* The **mention mode** predicts mentions
* The **ner mode** assigns each predicted mentions a entity types
* The **head mode** assigns each predicted mentions a mention head

## To use pre-trained models
The pre-trained models can be download from [this link](https://www.dropbox.com/s/perxerqcteghiwn/dalinnpipelinemodels.zip?dl=0)

To use the entire pipeline:
```
python test_pipe.py --mention_model mention_model_path --ner_model ner_model_path --head_model head_model_path --input input_path --output output_path
```
If you only want to use one of the mode:
```
python test.py --model model_path --input input_path --output output_path
```

## To train you own models
To train your own model you need first download the pre-trained [GloVe embeddings](https://nlp.stanford.edu/projects/glove/).

Then use the following command:
```
python train.py --train train_set --dev dev_set --test test_set --mode \[mention|ner|head\] --pre_emb glove_path
```
Since it is a pipeline system, three models are trained separately.
