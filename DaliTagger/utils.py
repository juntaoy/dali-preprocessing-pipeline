import os,time,logging
import re
import numpy as np

models_path = "./models"

MODE_MENTION, MODE_NER, MODE_HEAD = 'mention','ner','head'

def get_name(parameters,prefix):
    """
    Generate a model name from its parameters.
    """

    l = []
    for k, v in parameters.items():
        if type(v) is str and "/" in v:
            l.append((k, v[::-1][:v[::-1].index('/')][::-1]))
        else:
            l.append((k, v))
    name = "-".join(["%s" % str(v).replace(',', '').replace('True', '1').replace('False', '0') for k, v in l])
    name = "".join(i for i in name if i not in "\/:*?<>|")
    name = prefix+name
    return name




def create_dico(item_list):
    """
    Create a dictionary of items from a list of list of items.
    """
    assert type(item_list) is list
    dico = {}
    for items in item_list:
        for item in items:
            if item not in dico:
                dico[item] = 1
            else:
                dico[item] += 1
    return dico


def create_mapping(dico):
    """
    Create a mapping (item to ID / ID to item) from a dictionary.
    Items are ordered by decreasing frequency.
    """
    sorted_items = sorted(dico.items(), key=lambda x: (-x[1], x[0]))
    id_to_item = {i: v[0] for i, v in enumerate(sorted_items)}
    item_to_id = {v: k for k, v in id_to_item.items()}
    return item_to_id, id_to_item


def zero_digits(s):
    """
    Replace every digit in a string by a zero.
    """
    return re.sub('\d', '0', s)


def range2iob(pred, s_len, id_to_tag):
    """
    :param pred: a list of prediction ranked by their index
    :param s_len: the sentence length
    :return: tags in IOB1 format
    """
    tags = ['O' for i in xrange(s_len)]
    for s, e, c in pred:
        c = id_to_tag[c]
        for i in xrange(s, e+1):
            if i == s and i > 0 and tags[i-1][2:] == c:
                t = 'B-'+c
            else:
                t= 'I-'+c
            tags[i] = t
    return tags



def iob2(tags):
    """
    Check that tags have a valid IOB format.
    Tags in IOB1 format are converted to IOB2.
    """
    for i, tag in enumerate(tags):
        if tag == 'O':
            continue
        split = tag.split('-')
        if len(split) != 2 or split[0] not in ['I', 'B']:
            return False
        if split[0] == 'B':
            continue
        elif i == 0 or tags[i - 1] == 'O':  # conversion IOB1 to IOB2
            tags[i] = 'B' + tag[1:]
        elif tags[i - 1][1:] == tag[1:]:
            continue
        else:  # conversion IOB1 to IOB2
            tags[i] = 'B' + tag[1:]
    return True


def iob_iobes(tags):
    """
    IOB -> IOBES
    """
    new_tags = []
    for i, tag in enumerate(tags):
        if tag == 'O':
            new_tags.append(tag)
        elif tag.split('-')[0] == 'B':
            if i + 1 != len(tags) and \
                    tags[i + 1].split('-')[0] == 'I':
                new_tags.append(tag)
            else:
                new_tags.append(tag.replace('B-', 'S-'))
        elif tag.split('-')[0] == 'I':
            if i + 1 < len(tags) and \
                    tags[i + 1].split('-')[0] == 'I':
                new_tags.append(tag)
            else:
                new_tags.append(tag.replace('I-', 'E-'))
        else:
            raise Exception('Invalid IOB format!')
    return new_tags


def iobes_iob(tags):
    """
    IOBES -> IOB
    """
    new_tags = []
    for i, tag in enumerate(tags):
        if tag.split('-')[0] == 'B':
            new_tags.append(tag)
        elif tag.split('-')[0] == 'I':
            new_tags.append(tag)
        elif tag.split('-')[0] == 'S':
            new_tags.append(tag.replace('S-', 'B-'))
        elif tag.split('-')[0] == 'E':
            new_tags.append(tag.replace('E-', 'I-'))
        elif tag.split('-')[0] == 'O':
            new_tags.append(tag)
        else:
            raise Exception('Invalid format!')
    return new_tags


def iob_ranges(tags):
    """
    IOB -> Ranges
    """
    ranges = []

    def check_if_closing_range():
        if i == len(tags) - 1 or tags[i + 1].split('-')[0] == 'O':
            ranges.append((begin, i, type))

    for i, tag in enumerate(tags):
        if tag.split('-')[0] == 'O':
            pass
        elif tag.split('-')[0] == 'B':
            begin = i
            type = tag.split('-')[1]
            check_if_closing_range()
        elif tag.split('-')[0] == 'I':
            check_if_closing_range()
    return ranges


def insert_singletons(words, singletons, p=0.5):
    """
    Replace singletons by the unknown word with a probability p.
    """
    new_words = []
    for word in words:
        if word in singletons and np.random.uniform() < p:
            new_words.append(0)
        else:
            new_words.append(word)
    return new_words


def _pad_sequences(sequences, pad_tok, max_length):
    """
    Args:
        sequences: a generator of list or tuple
        pad_tok: the char to pad with

    Returns:
        a list of list where each sublist has same length
    """
    sequence_padded, sequence_length = [], []

    for seq in sequences:
        seq = list(seq)
        seq_ = seq[:max_length] + [pad_tok]*max(max_length - len(seq), 0)
        sequence_padded +=  [seq_]
        sequence_length += [min(len(seq), max_length)]

    return sequence_padded, sequence_length


def pad_sequences(sequences, pad_tok, nlevels=1,max_len=-1):
    """
    Args:
        sequences: a generator of list or tuple
        pad_tok: the char to pad with
        nlevels: "depth" of padding, for the case where we have characters ids

    Returns:
        a list of list where each sublist has same length

    """
    if nlevels == 1:
        max_length = max(map(lambda x : len(x), sequences))
        max_length = max(max_length,max_len)
        sequence_padded, sequence_length = _pad_sequences(sequences,
                                            pad_tok, max_length)

    elif nlevels == 2:
        max_length_word = max([max(map(lambda x: len(x), seq))
                               for seq in sequences])
        sequence_padded, sequence_length = [], []
        for seq in sequences:
            # all words are same length now
            sp, sl = _pad_sequences(seq, pad_tok, max_length_word)
            sequence_padded += [sp]
            sequence_length += [sl]

        max_length_sentence = max(map(lambda x : len(x), sequences))
        sequence_padded, _ = _pad_sequences(sequence_padded,
                [pad_tok]*max_length_word, max_length_sentence)
        sequence_length, _ = _pad_sequences(sequence_length, 0,
                max_length_sentence)

    return sequence_padded, sequence_length


def select_candidate(data, sid, keepall=False):
    """
    Randomly selecting negative training examples by d_rate and o_rate
    The positive examples are always included
    """
    start = data['start']
    end = data['end']
    tags = data['tags']
    cat = data['cat']
    heads = data['heads']
    d_rate = data['d_rate']
    o_rate = data['o_rate']
    s, e, t, att,h = [], [], [], [],[]
    for i in xrange(len(start)):
        if keepall:
            keep = True
        else:
            keep = False
            if cat[i] == 0:  # positive
                keep = True
            elif cat[i] == 1 and np.random.uniform() < o_rate:  # overlap
                keep = True
            elif cat[i] == 2 and np.random.uniform() < d_rate:  # disjoint
                keep = True

        if keep:
            s.append([sid,start[i]])
            e.append([sid,end[i]])
            t.append(tags[i])
            h.append(heads[i])
            att.append([[sid,k] for k in xrange(start[i],end[i]+1)])

    return s, e, t,h, att, data['marks']



def _merge_adjacient( estimate ):
    best, i = set(), 0
    while i < len(estimate):
        j = i + 1
        while j < len(estimate):
            if estimate[j][0] + 1 == estimate[j - 1][1] and \
                    estimate[j][2] == estimate[j - 1][2]:
                j += 1
            else:
                break
        assert estimate[i][2] == estimate[j - 1][2]
        best.add((estimate[i][0], estimate[j - 1][1], estimate[i][2]))
        i = j
    estimate = best
    return estimate



def get_nested_mentions(y_preds):
    """
    nested mention
    """
    y_preds.sort(key=lambda x: x[2], reverse=True)
    estimate = []
    for (b, e, s) in y_preds:
        if len(estimate) == 0:
            estimate.append((b, e))
        else:
            save = True
            for (bs, es) in estimate:
                if b < bs <= e < es or bs < b <= es < e:
                    save = False
                    break
            if save:
                estimate.append((b, e))
    estimate.sort(key=lambda x: x[0])
    return estimate




def parse_lr_method(lr_method):
    lr_method = lr_method.lower()
    if "-" in lr_method:
        lr_method_name = lr_method[:lr_method.find('-')]
        lr = float(lr_method[lr_method.find('-'):].split('_')[1])
    else:
        lr_method_name = lr_method.lower()
        lr = -1
    if lr_method_name == 'sgd':
        lr = 0.01 if lr < 0 else lr
    elif lr_method_name == 'adagrad':
        lr = 1.0 if lr < 0 else lr
    elif lr_method_name == 'adam':
        lr = 0.001 if lr < 0 else lr
    elif lr_method_name == 'rmsprop':
        lr = 0.001 if lr < 0 else lr
    else:
        raise ("Not implemented learning method: %s" % lr_method)
    return lr_method_name, lr

def time_used(start_time):
    curr_time = time.time()
    used_time = curr_time-start_time
    h = used_time // 3600
    used_time -= h * 3600
    m = used_time // 60
    s = used_time - 60 * m
    return "%d h %d m %d s" % (h, m, s)

