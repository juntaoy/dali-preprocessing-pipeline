import os, time,logging
import numpy as np
import optparse
import itertools
from collections import OrderedDict
from utils import models_path, time_used, parse_lr_method,MODE_MENTION,MODE_HEAD,MODE_NER
from loader import word_mapping, char_mapping, tag_mapping
from loader import prepare_dataset, load_ner2line_sentences
from loader import augment_with_pretrained
from model import Model

start_time = time.time()
optparser = optparse.OptionParser()
optparser.add_option(
    "--train", default="",
    help="Train set location"
)
optparser.add_option(
    "--dev", default="",
    help="Dev set location"
)
optparser.add_option(
    "--test", default="",
    help="Test set location"
)
optparser.add_option(
    "--mode", default="mention",
    help="Tagging scheme (mention,ner or head)"
)
optparser.add_option(
    "--lower", default=False, action="store_true",
    help="Lowercase words (this will not affect character inputs)"
)
optparser.add_option(
    "--zeros", default=False, action="store_true",
    help="Replace digits with 0"
)
optparser.add_option(
    "--char_dim", default="25",
    type='int', help="Char embedding dimension"
)
optparser.add_option(
    "--char_lstm_dim", default="25",
    type='int', help="Char LSTM hidden layer size"
)
optparser.add_option(
    "--word_dim", default="300",
    type='int', help="Token embedding dimension"
)
optparser.add_option(
    "--word_lstm_dim", default="300",
    type='int', help="Token LSTM hidden layer size"
)
optparser.add_option(
    "--word_lstm_layer", default="2",
    type='int', help="The number of lstm layers to be used in the system."
)
optparser.add_option(
    "--relu_layer", default="2",
    type='int', help="The number of relu layers to be used in the system."
)
optparser.add_option(
    "--pre_emb", default="data/glove.6B/glove.6B.300d.txt",
    help="Location of pretrained embeddings"
)
optparser.add_option(
    "--all_emb", default=False, action="store_true",
    help="Load all embeddings"
)
optparser.add_option(
    "--cap_dim", default="0",
    type='int', help="Capitalization feature dimension (0 to disable)"
)
optparser.add_option(
    "--len_dim", default="25",
    type='int', help="markable length feature dimension (0 to disable)"
)
optparser.add_option(
    "--dropout", default="0.5",
    type='float', help="Droupout on the input (1 = no dropout)"
)
optparser.add_option(
    "--hidden_dropout", default="0.8",
    type='float', help="Droupout on the hidden layer (1 = no dropout)"
)
optparser.add_option(
    "--lr_method", default="adam-lr_.001",
    help="Learning method (SGD, Adadelta, Adam..) with learning rate"
)
optparser.add_option(
    "--decay_frequency", default="100",
    type='int', help="Number of steps in which should decay the learning rate"
)
optparser.add_option(
    "--decay_rate", default="0.999",
    type="float", help="The decay rate for learning rate"
)
optparser.add_option(
    "--reload", default=False, action="store_true",
    help="Reload the last saved model"
)
optparser.add_option(
    "--threshold", default="0.5",
    help = "The minimum score threshold for output the algorithm, could be multiple, e.g: 0.5,0.6"
)
optparser.add_option(
    "--batch_size", default="100",
    type="int", help="The size of minibatches"
)
optparser.add_option(
    "--max_len", default="20",
    type="int", help="The maximum length of the candidate mention"
)
optparser.add_option(
    "--overlap_rate", default="0.5",
    type="float", help="The rate of overlap candidates should be used, the rate for disjoint candidates are 1 - overlap_rate"
)
optparser.add_option(
    "--negative_ratio", default="1.0",
    type="float", help="The ratio of how many negative examples should be include in the training."
)
optparser.add_option(
    "--nepochs", default="500",
    type="int", help="The maximum epochs to train."
)
optparser.add_option(
    "--f_beta", default="1",
    type="float", help="The beta for calculate f_beta score, used to select best model"
)
opts = optparser.parse_args()[0]

# Parse parameters
parameters = OrderedDict()
parameters['mode'] = opts.mode
parameters['train'] = opts.train
parameters['nepochs'] = opts.nepochs
parameters['lower'] = opts.lower
parameters['zeros'] = opts.zeros
parameters['char_dim'] = opts.char_dim
parameters['char_lstm_dim'] = opts.char_lstm_dim
parameters['word_dim'] = opts.word_dim
parameters['word_lstm_dim'] = opts.word_lstm_dim
parameters['word_lstm_layer'] = opts.word_lstm_layer
parameters['relu_layer'] = opts.relu_layer
parameters['pre_emb'] = opts.pre_emb
parameters['all_emb'] = opts.all_emb
parameters['cap_dim'] = opts.cap_dim
parameters['len_dim'] = opts.len_dim
parameters['dropout'] = opts.dropout
parameters['hidden_dropout'] = opts.hidden_dropout
parameters['lr_method'], \
parameters['learning_rate'] = parse_lr_method(opts.lr_method)
parameters['decay_frequency'] = opts.decay_frequency
parameters['decay_rate'] = opts.decay_rate
parameters['reload'] = opts.reload
parameters['threshold'] = opts.threshold
parameters['b_size'] = opts.batch_size
parameters['max_len'] = opts.max_len
parameters['overlap_rate'] = opts.overlap_rate
parameters['negative_ratio'] = opts.negative_ratio
parameters['f_beta'] = opts.f_beta

# Check parameters validity
assert os.path.isfile(opts.train)
assert os.path.isfile(opts.dev)
assert os.path.isfile(opts.test)
assert parameters['mode'] in ['mention','ner','head']
assert parameters['nepochs'] > 0
for th in parameters['threshold'].split(','):
    assert 0. <= float(th) < 1.0
assert 0. < parameters['overlap_rate'] < 1.0
assert -1 == parameters['max_len'] or parameters['max_len'] >= 1
assert 0 < parameters['negative_ratio']
assert 1 <= parameters['word_lstm_layer'] <= 4 # maximum 4 layers
assert 1 <= parameters['relu_layer'] <= 4 # maximum 4 layers
assert parameters['char_dim'] > 0 or parameters['word_dim'] > 0
assert 0. < parameters['dropout'] <= 1.0
assert 0. < parameters['hidden_dropout'] <= 1.0
assert 1 <= parameters['b_size']
assert not parameters['all_emb'] or parameters['pre_emb']
assert not parameters['pre_emb'] or parameters['word_dim'] > 0
assert not parameters['pre_emb'] or os.path.isfile(parameters['pre_emb'])

if not os.path.exists(models_path):
    os.makedirs(models_path)

# Initialize model
model = Model(parameters=parameters, models_path=models_path)
logging.basicConfig(format='%(message)s',level=logging.INFO, filename=model.log_path)
logging.info( "Model location: %s" % model.model_path)
logging.info('Model Configurations:')
for k, v in parameters.items():
    logging.info('%s=%s' % (k, v))

# Data parameters
lower = parameters['lower']
zeros = parameters['zeros']
#tag_scheme = parameters['tag_scheme']

# Load sentences
train_sentences, train_m = load_ner2line_sentences(opts.train, lower, zeros)
dev_sentences, dev_m = load_ner2line_sentences(opts.dev, lower, zeros)
test_sentences, test_m= load_ner2line_sentences(opts.test, lower, zeros)

# Create a dictionary / mapping of words
# If we use pretrained embeddings, we add them to the dictionary.
if parameters['pre_emb']:
    dico_words_train = word_mapping(train_sentences, lower)[0]
    dico_words, word_to_id, id_to_word, word_embeddings = augment_with_pretrained(
        dico_words_train.copy(),
        parameters['pre_emb'],
        list(itertools.chain.from_iterable(
            [s['tokens'] for s in dev_sentences + test_sentences])
        ) if not parameters['all_emb'] else None,
        parameters['word_dim']
    )
else:
    dico_words, word_to_id, id_to_word = word_mapping(train_sentences, lower)
    dico_words_train = dico_words
    logging.info("Random initialize the word embeddings.")
    word_embeddings = np.random.rand(len(id_to_word),parameters['word_dim'])

# Create a dictionary and a mapping for words / POS tags / tags
dico_chars, char_to_id, id_to_char = char_mapping(train_sentences)
dico_tags, tag_to_id, id_to_tag = tag_mapping(train_sentences,parameters['mode'])

# Index data
train_data = prepare_dataset(
    train_sentences, word_to_id, char_to_id, tag_to_id, parameters['mode'],lower,
    parameters['overlap_rate'], parameters['negative_ratio'], parameters['max_len']
)
dev_data = prepare_dataset(
    dev_sentences, word_to_id, char_to_id, tag_to_id, parameters['mode'],lower,
    parameters['overlap_rate'], parameters['negative_ratio'], parameters['max_len']
)
test_data = prepare_dataset(
    test_sentences, word_to_id, char_to_id, tag_to_id,parameters['mode'], lower,
    parameters['overlap_rate'], parameters['negative_ratio'], parameters['max_len']
)

logging.info( "%i / %i / %i sentences in train / dev / test." % (
    len(train_data), len(dev_data), len(test_data)))
logging.info("%i / %i / %i mentions in train / dev / test." % (
        train_m, dev_m, test_m))
# Save the mappings to disk
logging.info( 'Saving the mappings to disk...')

if parameters['mode'] == MODE_MENTION:
    o_id = tag_to_id['O']
else:
    o_id = 0
model.save_mappings(id_to_word, id_to_char, id_to_tag,o_id)


model.build(word_embeddings)

#
# Train network
#
singletons = set([word_to_id[k] for k, v
                  in dico_words_train.items() if v == 1])


logging.info("Start training... time used so far: %s" % time_used(start_time))

model.train(train_data,dev_data,test_data,singletons)
model.close_session()

logging.info("Total time used %s" % time_used(start_time))