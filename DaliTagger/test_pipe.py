import os, time, codecs, optparse
import logging
from loader import prepare_dataset,load_ner2line_sentences
from model import Model
from utils import time_used,MODE_MENTION,MODE_NER,MODE_HEAD



optparser = optparse.OptionParser()
optparser.add_option(
    "--mention_model", default="",
    help="Model location"
)
optparser.add_option(
    "--ner_model", default="",
    help="Model location"
)
optparser.add_option(
    "--head_model", default="",
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

assert os.path.isdir(opts.mention_model)
assert os.path.isdir(opts.ner_model)
assert os.path.isdir(opts.head_model)
assert os.path.isfile(opts.input)
log_path = opts.output+'.log'
if os.path.isfile(log_path):
    os.remove(log_path)
logging.basicConfig(format='%(message)s', level=logging.INFO, filename=log_path)

s_time = time.time()

gold_sentences, _ = load_ner2line_sentences(opts.input, lower=False, zeros=False)

gold_marks = []


pred_marks = []
tag_len = 0
id_to_tag = {}
input = opts.input
for mode in [MODE_MENTION,MODE_NER,MODE_HEAD]:
    model_path = opts.mention_model if mode == MODE_MENTION else (opts.ner_model if mode==MODE_NER else opts.head_model)
    model = Model(model_path=model_path)
    parameters = model.parameters
    assert parameters['mode'] == mode


    logging.info("Loading model...")
    for k, v in parameters.items():
        logging.info('%s=%s' % (k, v))

    model.build()
    model.reload()

    # Load reverse mappings
    word_to_id, char_to_id, tag_to_id = [
        {v: k for k, v in x.items()}
        for x in [model.id_to_word, model.id_to_char, model.id_to_tag]
    ]


    logging.info("Reading test data from %s..." % input)

    lower = parameters['lower']
    zeros = parameters['zeros']

    test_sentences, len_mention = load_ner2line_sentences(input, lower, zeros)



    test_data = prepare_dataset(
        test_sentences, word_to_id, char_to_id, tag_to_id, parameters['mode'],lower,
        parameters['overlap_rate'], parameters['negative_ratio'], parameters['max_len'], keepAll=True
    )

    if mode==MODE_NER:
        tag_len = len(model.id_to_tag)
        id_to_tag = model.id_to_tag
        for sents in gold_sentences:
            gold_marks.append([(b,e,tag_to_id[c], h) for (b,e,c,h) in sents['marks']])

    logging.info("%d sentences find in test dataset" % len(test_data))
    logging.info("%d mentions find in test dataset" % len_mention)


    t_time = time.time()
    logging.info("Tagging...")

    _, _, _, _,_, preds,h_score = model.eval(test_data)

    pred_marks = preds

    f_output = codecs.open(opts.output, 'w', 'utf-8')

    logging.info("Time used for tagging:%s" % time_used(t_time))
    if mode == MODE_HEAD:
        for sent, pred, sc in zip(gold_sentences, preds, h_score):
            f_output.write(" ".join(w for w in sent['tokens']))
            f_output.write("\n")
            f_output.write(" ".join(str(s) for s in sc[:len(sent['tokens'])]))
            f_output.write("\n")
            f_output.write(" ".join(("%d,%d,%s,%d" % (s, e + 1, model.id_to_tag[c], h)) for (s, e, c, h) in pred))
            f_output.write("\n\n")
    else:
        for sent, pred in zip(gold_sentences, preds):
            f_output.write(" ".join(w for w in sent['tokens']))
            f_output.write("\n")
            f_output.write(" ".join( ("%d,%d,%s,%d" % (s,e+1,model.id_to_tag[c],h)) for (s,e,c,h) in pred))
            f_output.write("\n\n")


    f_output.close()

    input = opts.output




#Evaluation on the final results
utp, ufp, ufn, hc, lc = 0,0,0,0,0
true_positive, false_positive, false_negative, len_mention \
    = [0 for i in xrange(tag_len)], \
      [0 for i in xrange(tag_len)], \
      [0 for i in xrange(tag_len)], \
      [0 for i in xrange(tag_len)]


for actual, estimate in zip(gold_marks,pred_marks):

    be_estimate = set([(b, e) for (b, e, c, h) in estimate])
    be_actual = set([(b, e) for (b, e, c, h) in actual])

    utp += len([(b, e) for (b, e) in be_estimate & be_actual])
    ufp += len([(b, e) for (b, e) in be_estimate - be_actual])
    ufn += len([(b, e) for (b, e) in be_actual - be_estimate])

    be_map_estimate, be_map_actual = {}, {}
    for b, e, c, h in estimate:
        be_map_estimate[(b, e)] = (c, h)
    for b, e, c, h in actual:
        be_map_actual[(b, e)] = (c, h)

    for m in be_estimate & be_actual:
        ce, he = be_map_estimate[m]
        ca, ha = be_map_actual[m]
        if he == ha:
            hc += 1
        if ce == ca:
            lc += 1

    bec_estimate = set([(b, e, c) for (b, e, c, h) in estimate])
    bec_actual = set([(b, e, c) for (b, e, c, h) in actual])

    for x in xrange(len(true_positive)):
        true_positive[x] += len([(b, e, c) for (b, e, c) in bec_estimate & bec_actual if c == x])
        false_positive[x] += len([(b, e, c) for (b, e, c) in bec_estimate - bec_actual if c == x])
        false_negative[x] += len([(b, e, c) for (b, e, c) in bec_actual - bec_estimate if c == x])
        len_mention[x] += len([(b, e, c) for (b, e, c) in bec_actual if c == x])


for x in xrange(len(true_positive)):
    if true_positive[x] != 0:
        precision = float(true_positive[x]) / float(true_positive[x] + false_positive[x])
        recall = float(true_positive[x]) / float(true_positive[x] + false_negative[x])
        fb1 = 2.0 * precision * recall / (precision + recall)
    else:
        precision, recall, fb1 = 0.0, 0.0, 0.0
    logging.info('%12s  precision: %.2f%%, recall: %.2f%%, FB1: %.2f, %d' % \
                 (id_to_tag[x], precision * 100, recall * 100, fb1 * 100, len_mention[x]))

true_positive, false_positive, false_negative, len_mention = \
    sum(true_positive), sum(false_positive), sum(false_negative), sum(len_mention)
if true_positive != 0:
    precision = float(true_positive) / float(true_positive + false_positive)
    recall = float(true_positive) / float(true_positive + false_negative)
    fb1 = 2.0 * precision * recall / (precision + recall)
else:
    precision, recall, fb1 = 0.0, 0.0, 0.0

if utp != 0:
    u_precision = float(utp) / float(utp + ufp)
    u_recall = float(utp) / float(utp + ufn)
    u_fb1 = 2.0 * u_precision * u_recall / (u_precision + u_recall)
    head_accuracy = float(hc) / float(utp)
    ner_accuracy = float(lc) / float(utp)
else:
    u_precision, u_recall, u_fb1, head_accuracy, ner_accuracy = 0.0, 0.0, 0.0, 0.0, 0.0

logging.info('%-12s  precision: %.2f%%, recall: %.2f%%, FB1: %.2f %d' % \
             ('OVERALL', precision * 100, recall * 100, fb1 * 100, len_mention))
logging.info('%-12s  precision: %.2f%%, recall: %.2f%%, FB1: %.2f %d' % \
             ('Mention', u_precision * 100, u_recall * 100, u_fb1 * 100, len_mention))
logging.info('%-12s: %.2f%% of total %d correct mentions' % \
             ('Head Accuracy', head_accuracy * 100, utp))
logging.info('%-12s: %.2f%% of total %d correct mentions' % \
             ('Ner Accuracy', ner_accuracy * 100, utp))

logging.info("Evaluation finished, the output is saved at:%s" % opts.output)
logging.info("Total time used:%s\n\n\n\n\n" % time_used(s_time))
