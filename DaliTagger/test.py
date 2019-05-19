import os, time, codecs, optparse
import logging
from loader import prepare_dataset,load_ner2line_sentences
from model import Model
from utils import time_used

optparser = optparse.OptionParser()
optparser.add_option(
    "--model", default="",
    help="Model location"
)
optparser.add_option(
    "--input", default="",
    help="Input file location"
)
optparser.add_option(
    "--output", default="",
    help="Output file location"
)
opts = optparser.parse_args()[0]

assert os.path.isdir(opts.model)
assert os.path.isfile(opts.input)


s_time = time.time()

model = Model(model_path=opts.model)
parameters = model.parameters


logging.basicConfig(format='%(message)s',level=logging.INFO, filename=model.log_path)
logging.info("Loading model...")

model.build()
model.reload()

# Load reverse mappings
word_to_id, char_to_id, tag_to_id = [
    {v: k for k, v in x.items()}
    for x in [model.id_to_word, model.id_to_char, model.id_to_tag]
]

logging.info("Reading test data from %s..." % opts.input)

lower = parameters['lower']
zeros = parameters['zeros']

test_sentences, len_mention = load_ner2line_sentences(opts.input, lower, zeros)
raw_sentences, _ = load_ner2line_sentences(opts.input,lower=False, zeros=False)



test_data = prepare_dataset(
    test_sentences, word_to_id, char_to_id, tag_to_id, parameters['mode'],lower,
    parameters['overlap_rate'], parameters['negative_ratio'], parameters['max_len']
)

logging.info("%d sentences find in test dataset" % len(test_data))
logging.info("%d mentions find in test dataset" % len_mention)


t_time = time.time()
logging.info("Tagging...")

_, _, fb, _,_, preds,_ = model.eval(test_data)

f_output = codecs.open(opts.output, 'w', 'utf-8')

logging.info("Time used for tagging:%s" % time_used(t_time))

for sent, pred in zip(raw_sentences, preds):
    f_output.write(" ".join(w for w in sent['tokens']))
    f_output.write("\n")
    f_output.write(" ".join( ("%d,%d,%s,%d" % (s,e+1,model.id_to_tag[c],h)) for (s,e,c,h) in pred))
    f_output.write("\n\n")


f_output.close()

logging.info("Evaluation finished, the output is saved at:%s" % opts.output)
logging.info("Total time used:%s\n\n\n\n\n" % time_used(s_time))
