import os,logging
import re
import codecs
import numpy as np
from utils import create_dico, create_mapping, zero_digits,MODE_MENTION,MODE_HEAD,MODE_NER

_MType = ["unknown","person","animate","concrete",
			"organization","space","time","numerical","plan","abstract"]
_MType_LEN = len(_MType)
def mention_type(s):
    for i, t in enumerate(_MType):
        if s == t:
            return t
    return _MType[0] # default unknown




def load_ner2line_sentences(path, lower=False, zeros = False):
    sentences = []
    tokens = []
    marks = []
    istoken = True
    len_mention = 0
    for line in codecs.open(path, 'r', 'utf-8'):
        line = line.strip()
        if line.startswith('#begin document'):
            logging.info('Skip line %s' % line)
            continue
        if len(line) > 0:
            if istoken:
                line = zero_digits(line) if zeros else line
                for tok in line.split():
                    tokens.append(tok)
                istoken = False
            else:
                for mark in line.split():
                    index = mark.split(',')
                    if len(index) < 4:
                        start, end, label, head = int(index[0]), int(index[1]), 'M', -1
                    else:
                        start, end, label, head = int(index[0]), int(index[1]), index[2],int(index[3])
                    label = mention_type(label)
                    if end <= start or end > len(tokens):
                        logging.info(
                            "WARNING: markable boundary out of sentence, sentence length: %d markable: %d, %d" % (
                            len(tokens), start, end))
                    else:
                        marks.append((start, end - 1, label,head))
                        len_mention += 1
        else:
            if len(tokens) > 0:
                sentences.append({'tokens': tokens, 'marks': marks})
            tokens = []
            marks = []
            istoken = True
    return sentences, len_mention


def word_mapping(sentences, lower):
    """
    Create a dictionary and a mapping of words, sorted by frequency.
    """
    words = [[x.lower() if lower else x for x in s['tokens']] for s in sentences]
    dico = create_dico(words)
    dico['<UNK>'] = 10000000
    word_to_id, id_to_word = create_mapping(dico)
    logging.info("Found %i unique words (%i in total)" % (
        len(dico), sum(len(x) for x in words)
    ))
    return dico, word_to_id, id_to_word


def char_mapping(sentences):
    """
    Create a dictionary and mapping of characters, sorted by frequency.
    """
    chars = ["".join([w for w in s['tokens']]) for s in sentences]
    dico = create_dico(chars)
    char_to_id, id_to_char = create_mapping(dico)
    logging.info( "Found %i unique characters" % len(dico))
    return dico, char_to_id, id_to_char


def tag_mapping(sentences,mode):
    """
    Create a dictionary and a mapping of tags, sorted by frequency.
    """
    tags = []
    if mode == MODE_MENTION:
        tags.append('M')
        tags.append('O')
    else:
      tags = [[mark[2] for mark in s['marks']] for s in sentences]

    dico = create_dico(tags)
    tag_to_id, id_to_tag = create_mapping(dico)
    logging.info( "Found %i unique named entity tags" % len(dico))
    return dico, tag_to_id, id_to_tag


def cap_feature(s):
    """
    Capitalization feature:
    0 = low caps
    1 = all caps
    2 = first letter caps
    3 = one capital (not first letter)
    """
    if s.lower() == s:
        return 0
    elif s.upper() == s:
        return 1
    elif s[0].upper() == s[0]:
        return 2
    else:
        return 3



def prepare_dataset(sentences, word_to_id, char_to_id, tag_to_id, mode, lower=False, overlap_rate=0.5, negative_ratio=1.0, max_len=-1,keepAll=False):
    """
    Prepare the dataset. Return a list of lists of dictionaries containing:
        - word indexes
        - word char indexes
        - tag indexes
    """
    def f(x): return x.lower() if lower else x
    data = []
    for s in sentences:
        str_words = s['tokens']
        words = [word_to_id[f(w) if f(w) in word_to_id else '<UNK>']
                 for w in str_words]
        # Skip characters that are not in the training set
        chars = [[char_to_id[c ] for c in w if c in char_to_id]
                 for w in str_words]
        caps = [cap_feature(w) for w in str_words]
        marks = []
        start,end,tags,cat,heads=[],[],[],[],[]
        #cat 0-positive, 1-overlap, 2-disjoint
        if mode == MODE_MENTION:
            out_id = tag_to_id['O']
            in_id = tag_to_id['M']
            mark_matrix = [[out_id for i in xrange(len(str_words))] for j in xrange(len(str_words))]
            for m in s['marks']:
                mark_matrix[m[0]][m[1]] = in_id
                marks.append((m[0],m[1]))

            cnp, cno, cnd = 0, 0, 0
            my_len = len(str_words) if max_len < 0 else max_len
            for i in xrange(len(str_words)):
                for j in xrange(i,min(i+my_len, len(str_words))):
                    c = 2 #disjoint by default
                    if mark_matrix[i][j] != out_id:
                        c = 0 #positive
                        cnp += 1
                    else:
                        for m in s['marks']:
                            if i < m[1] and m[0] < j:
                                c = 1 #overlap
                                cno += 1
                                break
                    start.append(i)
                    end.append(j)
                    tags.append(mark_matrix[i][j])
                    cat.append(c)
                    heads.append(0)

            disjoint_rate = 1-overlap_rate
            cnd = len(cat) - cnp - cno

            if cnp == 0:
                d_rate = 2 / cnd if cnd > 0 else .0
                o_rate = 2 / cno if cno > 0 else .0
                #to make sure at least something is included in the training
            else:
                cnp *= negative_ratio
                d_rate = cnp * disjoint_rate / cnd if cnd > 0 else .0
                o_rate = cnp * overlap_rate / cno if cno > 0 else .0
        else:
            for m in s['marks']:
                if not keepAll and max_len > 0 and (m[1]-m[0]+1 > max_len):
                    continue
                if not keepAll and mode == MODE_HEAD and (m[3] > m[1] or m[3] < m[0]):
                    continue
                marks.append((m[0], m[1], tag_to_id[m[2]], m[3]))
                start.append(m[0])
                end.append(m[1])
                tags.append(tag_to_id[m[2]])
                heads.append(m[3]-m[0] )
                cat.append(0)#all positive
            d_rate = 1.0
            o_rate = 1.0

        data.append({
            'str_words': str_words,
            'words': words,
            'chars': chars,
            'caps': caps,
            'tags': tags,
            'start':start,
            'end':end,
            'heads':heads,
            'cat':cat,
            'd_rate':d_rate,
            'o_rate':o_rate,
            'marks':marks
        })
    return data


def augment_with_pretrained(dictionary, ext_emb_path, words,word_dim):
    """
    Augment the dictionary with words that have a pretrained embedding.
    If `words` is None, we add every word that has a pretrained embedding
    to the dictionary, otherwise, we only add the words that are given by
    `words` (typically the words in the development and test sets.)
    """
    logging.info( 'Loading pretrained embeddings from %s...' % ext_emb_path)
    assert os.path.isfile(ext_emb_path)

    # Load pretrained embeddings from file
    pretrained = set([
        line.rstrip().split()[0].strip()
        for line in codecs.open(ext_emb_path, 'r', 'utf-8')
        if len(line.strip()) > 0
    ])

    # We either add every word in the pretrained file,
    # or only words given in the `words` list to which
    # we can assign a pretrained embedding
    if words is None:
        for word in pretrained:
            if word not in dictionary:
                dictionary[word] = 0
    else:
        for word in words:
            if any(x in pretrained for x in [
                word,
                word.lower(),
                re.sub('\d', '0', word.lower())
            ]) and word not in dictionary:
                dictionary[word] = 0

    word_to_id, id_to_word = create_mapping(dictionary)
    n_words = len(id_to_word)
    word_embeddings = np.random.rand(n_words,word_dim)
    pretrained = {}
    emb_invalid = 0
    for i, line in enumerate(codecs.open(ext_emb_path, 'r', 'utf-8')):
        line = line.rstrip().split()
        if len(line) == word_dim + 1:
            pretrained[line[0]] = np.array(
                [float(x) for x in line[1:]]
            ).astype(np.float32)
        else:
            emb_invalid += 1
    if emb_invalid > 0:
        logging.info( 'WARNING: %i invalid lines' % emb_invalid)

    c_found = 0
    c_lower = 0
    c_zeros = 0
    # Lookup table initialization
    for i in xrange(n_words):
        word = id_to_word[i]
        if word in pretrained:
            word_embeddings[i] = pretrained[word]
            c_found += 1
        elif word.lower() in pretrained:
            word_embeddings[i] = pretrained[word.lower()]
            c_lower += 1
        elif re.sub('\d', '0', word.lower()) in pretrained:
            word_embeddings[i] = pretrained[
                re.sub('\d', '0', word.lower())
            ]
            c_zeros += 1
    logging.info( 'Loaded %i pretrained embeddings.' % len(pretrained))
    logging.info(('%i / %i (%.4f%%) words have been initialized with '
           'pretrained embeddings.') % (
              c_found + c_lower + c_zeros, n_words,
              100. * (c_found + c_lower + c_zeros) / n_words
          ))
    logging.info(('%i found directly, %i after lowercasing, '
           '%i after lowercasing + zero.') % (
              c_found, c_lower, c_zeros
          ))


    return dictionary, word_to_id, id_to_word, word_embeddings
