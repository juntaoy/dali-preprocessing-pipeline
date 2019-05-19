import os, time, logging
import cPickle
import tensorflow as tf
import numpy as np

from utils import get_name, time_used, pad_sequences, \
    get_nested_mentions, insert_singletons,select_candidate,\
    MODE_MENTION,MODE_NER,MODE_HEAD

class Model(object):
    """
    Network architecture
    """

    def __init__(self, parameters=None, models_path=None, model_path=None,prefix='model-'):
        """
        Initialize the model. We either provide the parameters and a path where
        we store the models, or the location of a trained model.
        """
        tf.reset_default_graph()

        if model_path is None:
            assert parameters and models_path
            # Create a name based on the parameters
            self.parameters = parameters
            if parameters.has_key('best_threshold'):
                parameters.pop('best_threshold')
            self.name = get_name(parameters,prefix)
            self.thresholds = [float(th) for th in parameters['threshold'].split(',')]
            self.parameters['best_threshold'] = self.thresholds[0]
            # Model location
            model_path = os.path.join(models_path, self.name)
            self.model_path = model_path
            self.tf_model_path = os.path.join(model_path,'tfmodel')
            self.parameters_path = os.path.join(model_path, 'parameters.pkl')
            self.mappings_path = os.path.join(model_path, 'mappings.pkl')
            self.log_path = os.path.join(model_path, 'training.log')
            if os.path.isfile(self.log_path):
                os.remove(self.log_path)
            # Create directory for the model if it does not exist
            if not os.path.exists(self.model_path):
                os.makedirs(self.model_path)
            self.save_parameters()
        else:
            assert parameters is None and models_path is None
            # Model location
            self.model_path = model_path
            self.tf_model_path = os.path.join(model_path,'tfmodel')
            self.parameters_path = os.path.join(model_path, 'parameters.pkl')
            self.mappings_path = os.path.join(model_path, 'mappings.pkl')
            self.log_path = os.path.join(self.model_path, 'eval.log')
            self.load_parameters()
            self.thresholds = [float(th) for th in str(self.parameters['threshold']).split(',')]
            self.reload_mappings()
        self.sess = None
        self.saver = None


    def save_parameters(self):
        # Save the parameters to disk
        with open(self.parameters_path, 'wb') as f:
            cPickle.dump(self.parameters, f)

    def load_parameters(self):
        # Load the parameters and the mappings from disk
        with open(self.parameters_path, 'rb') as f:
            self.parameters = cPickle.load(f)

    def save_mappings(self, id_to_word, id_to_char, id_to_tag, o_id):
        """
        We need to save the mappings if we want to use the model later.
        """
        self.id_to_word = id_to_word
        self.id_to_char = id_to_char
        self.id_to_tag = id_to_tag
        self.o_id = o_id
        with open(self.mappings_path, 'wb') as f:
            mappings = {
                'id_to_word': self.id_to_word,
                'id_to_char': self.id_to_char,
                'id_to_tag': self.id_to_tag,
                'o_id':self.o_id
            }
            cPickle.dump(mappings, f)

    def reload_mappings(self):
        """
        Load mappings from disk.
        """
        with open(self.mappings_path, 'rb') as f:
            mappings = cPickle.load(f)
        self.id_to_word = mappings['id_to_word']
        self.id_to_char = mappings['id_to_char']
        self.id_to_tag = mappings['id_to_tag']
        self.o_id = mappings['o_id']


    def reload(self):
        """
        Reload weights into session
        """
        logging.info("Reloading the trained model...")
        self.saver.restore(self.sess, self.tf_model_path)
        self.sess.run(self.reset_global_step)


    def save(self):
        """Saves session = weights"""
        self.saver.save(self.sess, self.tf_model_path)


    def close_session(self):
        """Closes the session"""
        self.sess.close()
        tf.reset_default_graph()


    def build(self, w_embeddings = np.array([])):
        """
        Build the network.
        """
        char_dim = self.parameters['char_dim']
        char_lstm_dim = self.parameters['char_lstm_dim']
        word_dim = self.parameters['word_dim']
        word_lstm_dim = self.parameters['word_lstm_dim']
        lr_method = self.parameters['lr_method']
        cap_dim = self.parameters['cap_dim']
        len_dim = self.parameters['len_dim']
        clip = 5.0

        self.global_step = tf.Variable(0, name="global_step", trainable=False)
        self.reset_global_step = tf.assign(self.global_step, 0)
        learning_rate = tf.train.exponential_decay(self.parameters["learning_rate"], self.global_step,
                                                   self.parameters["decay_frequency"], self.parameters["decay_rate"],
                                                   staircase=True)


        # Training parameters
        n_words = len(self.id_to_word)
        n_chars = len(self.id_to_char)
        n_tags = len(self.id_to_tag)

        # Number of capitalization features
        if cap_dim:
            n_cap = 4

        if len_dim:
            n_len = self.parameters['max_len']


        # Network variables

        # shape = (batch size, max length of sentence in batch)
        self.word_ids = tf.placeholder(tf.int32, shape=[None, None],
                                       name="word_ids")
        # shape = (batch size)
        self.sent_lengths = tf.placeholder(tf.int32, shape=[None],
                                               name="sent_lengths")


        # shape = (number of candidates in batch, 2 [batch id,word_id])
        self.start_ids = tf.placeholder(tf.int32, shape=[None, None],
                                     name="start_ids")
        self.end_ids = tf.placeholder(tf.int32, shape=[None, None],
                                      name="end_ids")
        self.dropout = tf.placeholder(dtype=tf.float32, shape=[],
                        name="dropout")
        self.hidden_dropout = tf.placeholder(dtype=tf.float32, shape=[],
                                          name="hidden_dropout")
        if char_dim:
            # shape = (batch size, max length of sentence, max length of word)
            self.char_ids = tf.placeholder(tf.int32, shape=[None, None, None],
                                           name="char_ids")
            # shape = (batch_size, max_length of sentence)
            self.word_lengths = tf.placeholder(tf.int32, shape=[None, None],
                                               name="word_lengths")

        #(number of candidates in the batch,  max_len, 2 [batch id, word_id])
        self.att_ids = tf.placeholder(tf.int32, shape=[None,None,None],
                                      name="att_ids")
        # shape = (number of candidates in the batch)
        self.markable_lengths = tf.placeholder(tf.int32, shape=[None],
                                               name="markable_lengths")

        #self.gold_labels = tf.placeholder(tf.bool,shape=[None,None],name="gold_labels")
        self.gold_labels = tf.placeholder(tf.int32,shape=[None],name="gold_labels")

        if cap_dim:
            self.cap_ids = tf.placeholder(tf.int32, shape=[None,None],
                                     name="cap_ids")

        if len_dim:
            self.len_ids = tf.placeholder(tf.int32, shape=[None])
        #
        # Word inputs
        #
        with tf.variable_scope("words"):
            if w_embeddings.size != 0:
                _word_embeddings = tf.Variable(
                    w_embeddings,
                    name="_word_embeddings",
                    dtype=tf.float32)
            else:
                _word_embeddings = tf.get_variable(
                    name="_word_embeddings",
                    dtype=tf.float32,
                    shape=[len(self.id_to_word), word_dim]
                )

            word_embeddings = tf.nn.embedding_lookup(_word_embeddings,
                                                     self.word_ids, name="word_embeddings")
            word_emb_dim = word_dim
        #
        # Chars inputs
        #
        with tf.variable_scope("chars"):
            if char_dim:
                # get char embeddings matrix
                _char_embeddings = tf.get_variable(
                    name="_char_embeddings",
                    dtype=tf.float32,
                    shape=[n_chars, char_dim])
                char_embeddings = tf.nn.embedding_lookup(_char_embeddings,
                                                         self.char_ids, name="char_embeddings")

                # put the time dimension on axis=1
                s = tf.shape(char_embeddings)
                char_embeddings = tf.reshape(char_embeddings,
                                             shape=[s[0] * s[1], s[-2], char_dim])
                word_lengths = tf.reshape(self.word_lengths, shape=[s[0] * s[1]])

                # bi lstm on chars
                char_lstm_for = tf.contrib.rnn.LSTMCell(char_lstm_dim,
                                                  state_is_tuple=True)
                char_lstm_rev = tf.contrib.rnn.LSTMCell(char_lstm_dim,
                                                  state_is_tuple=True)
                _output = tf.nn.bidirectional_dynamic_rnn(
                    char_lstm_for, char_lstm_rev, char_embeddings,
                    sequence_length=word_lengths, dtype=tf.float32)

                # read and concat output
                _, ((_, char_for_output), (_, char_rev_output)) = _output
                char_output = tf.concat([char_for_output, char_rev_output], axis=-1)

                # shape = (batch size, max sentence length, char hidden size)
                char_output = tf.reshape(char_output,
                                    shape=[s[0], s[1], 2 * char_lstm_dim])

                word_embeddings = tf.concat([word_embeddings, char_output], axis=-1)
                word_emb_dim += 2 * char_dim
        #
        # Capitalization feature
        #
        with tf.variable_scope("caps"):
            if cap_dim:
                _cap_embeddings = tf.get_variable(
                    name="_cap_embeddings",
                    dtype=tf.float32,
                    shape=[n_cap, cap_dim]
                )
                cap_embeddings = tf.nn.embedding_lookup(_cap_embeddings,
                                                        self.cap_ids, name="cap_embeddings")
                word_embeddings = tf.concat([word_embeddings, cap_embeddings], axis=-1)
                word_emb_dim+=cap_dim

        self.word_embeddings = tf.nn.dropout(word_embeddings, self.dropout)

        with tf.variable_scope("bi-lstm"):
            # LSTM for words
            num_layer = self.parameters['word_lstm_layer']
            word_lstm_for, word_lstm_rev = [],[]
            for _ in xrange(num_layer):
                word_lstm_for.append(tf.contrib.rnn.DropoutWrapper(tf.contrib.rnn.LSTMCell(word_lstm_dim),
                                                                   state_keep_prob = self.hidden_dropout,
                                                                   variational_recurrent = True,
                                                                   dtype = tf.float32))
                word_lstm_rev.append(tf.contrib.rnn.DropoutWrapper(tf.contrib.rnn.LSTMCell(word_lstm_dim),
                                                                   state_keep_prob = self.hidden_dropout,
                                                                   variational_recurrent = True,
                                                                   dtype = tf.float32))

            word_output, _, _ = tf.contrib.rnn.stack_bidirectional_dynamic_rnn(
                word_lstm_for, word_lstm_rev, self.word_embeddings,
                sequence_length=self.sent_lengths, dtype=tf.float32
            )

        word_output = tf.nn.dropout(word_output,self.hidden_dropout)

        candidate_starts = tf.gather_nd(word_output, self.start_ids, name="candidate_starts")
        candidate_ends = tf.gather_nd(word_output, self.end_ids, name="candidate_ends")

        pair_output = tf.concat([candidate_starts,candidate_ends],axis=-1)

        pair_dim = 4 * word_lstm_dim

        with tf.variable_scope("attention"):
            m_sent = tf.shape(word_output)[1]
            w_p = tf.get_variable("w_p", dtype=tf.float32,
                                  shape=[2*word_lstm_dim, 1])
            b_p = tf.get_variable("b_p",dtype=tf.float32,
                                  shape=[1], initializer=tf.zeros_initializer)

            flat_word_output = tf.reshape(word_output,[-1,2*word_lstm_dim])

            projection_output = tf.matmul(flat_word_output, w_p) + b_p
            projection_output = tf.reshape(projection_output, [-1, m_sent])


            candidate_word_emb = tf.gather_nd(self.word_embeddings,
                                         self.att_ids, name="candidate_context") #[number of candidates in batch, max_len, 2*word_lstm_dim]
            candidate_context_prob = tf.gather_nd(projection_output,
                                                  self.att_ids, name="candidate_context_prob") #[number of candidates in batch, max_len]

            #apply a mask to prob
            m_mark = tf.shape(candidate_word_emb)[1]

            candidate_context_prob += tf.log(tf.sequence_mask(self.markable_lengths,m_mark,dtype=tf.float32))
            candidate_context_att = tf.nn.softmax(candidate_context_prob)

            candidate_context_att = tf.reshape(candidate_context_att,[-1, m_mark,1])

            attention_output = tf.multiply(candidate_word_emb, candidate_context_att)
            attention_output = tf.reduce_sum(attention_output,axis=1) #[number of candidates, 2*word_lstm_dim]

            pair_output = tf.concat([pair_output,attention_output], axis=-1)
            pair_dim += word_emb_dim

        if self.parameters['mode'] == MODE_HEAD:
            self.preds = tf.argmax(candidate_context_prob, axis=1, output_type=tf.int32)
            self.head_socres = projection_output
            costs = tf.nn.sparse_softmax_cross_entropy_with_logits(logits=candidate_context_prob, labels=self.gold_labels)
            self.cost = tf.reduce_mean(costs)
        else:
            with tf.variable_scope("len_feature"):
                if len_dim:
                    _len_embeddings = tf.get_variable(
                        name="_len_embeddings",
                        dtype=tf.float32,
                        shape=[n_len, len_dim]
                    )
                    len_embeddings = tf.nn.embedding_lookup(_len_embeddings,
                                                            self.len_ids, name="len_embeddings")
                    pair_output = tf.concat([pair_output, len_embeddings], axis=-1)
                    pair_dim += len_dim

            with tf.variable_scope("relu"):
                w_h1 = tf.get_variable("w_h1", dtype=tf.float32,
                                       shape=[pair_dim, word_lstm_dim])
                b_h1 = tf.get_variable("b_h1", dtype=tf.float32,
                                       shape=[word_lstm_dim], initializer=tf.zeros_initializer)
                hidden_output = tf.nn.dropout(tf.nn.relu6(tf.matmul(pair_output, w_h1) + b_h1),
                                              self.hidden_dropout)

                num_hidden = self.parameters['relu_layer']
                for i in xrange(num_hidden-1):
                    w_h = tf.get_variable(("w_h%d" % (i+2)), dtype=tf.float32,
                                            shape=[word_lstm_dim, word_lstm_dim])
                    b_h = tf.get_variable(("b_h%d" % (i+2)), dtype=tf.float32,
                                   shape=[word_lstm_dim], initializer=tf.zeros_initializer)
                    hidden_output = tf.nn.dropout(tf.nn.relu6(tf.matmul(hidden_output, w_h) + b_h),
                                                  self.hidden_dropout)


            with tf.variable_scope("softmax"):
                w_sm = tf.get_variable("w_sm", dtype=tf.float32,
                                       shape=[word_lstm_dim,n_tags])
                b_sm = tf.get_variable("b_sm", dtype=tf.float32,
                                       shape=[n_tags], initializer=tf.zeros_initializer)

                logits = tf.matmul(hidden_output, w_sm)+ b_sm

                self.scores = tf.nn.softmax(logits,axis=1)
                self.preds = tf.argmax(logits, axis=-1,output_type= tf.int32)
            #head_type = tf.concat([candidate_context_prob,logits],axis=1)
            #head_type = logits

            #gold_scores = head_type + tf.log(tf.to_float(self.gold_labels))
            #marginalized_gold_scores = tf.reduce_logsumexp(gold_scores, [1])
            #log_norm = tf.reduce_logsumexp(head_type, [1])
            #costs = log_norm - marginalized_gold_scores


            #self.cost = tf.reduce_sum(costs)

            costs = tf.nn.sparse_softmax_cross_entropy_with_logits(logits=logits, labels=self.gold_labels)
            self.cost = tf.reduce_mean(costs)


        optimizers = {
            'sgd': tf.train.GradientDescentOptimizer,
            'adagrad':tf.train.AdagradOptimizer,
            'adam':tf.train.AdamOptimizer,
            'rmsprop':tf.train.RMSPropOptimizer
        }

        optimizer = optimizers[lr_method](learning_rate)



        grads, vs = zip(*optimizer.compute_gradients(self.cost))
        grads, gnorm = tf.clip_by_global_norm(grads, clip)
        self.train_op = optimizer.apply_gradients(zip(grads, vs),global_step=self.global_step)


        #
        #Defines self.sess and initialize the variables
        #
        logging.info("Initializing tf session")
        config = tf.ConfigProto()
        config.gpu_options.allow_growth = True
        config.allow_soft_placement = True
        self.sess = tf.Session(config=config)
        self.sess.run(tf.global_variables_initializer())
        self.saver = tf.train.Saver()


    def _pad_feed_dict(self, word_ids,
                       char_ids, cap_ids,
                       start_ids, end_ids,
                       gold_labels, att_ids,
                       dropout, hidden_dropout):
        fd = {}
        word_ids, sent_lenghts = pad_sequences(word_ids,0)
        fd[self.word_ids] = word_ids
        fd[self.sent_lengths] = sent_lenghts

        if self.parameters['char_dim']:
            char_ids, word_lenghts = pad_sequences(char_ids,0,2)
            fd[self.char_ids] = char_ids
            fd[self.word_lengths] = word_lenghts
        if self.parameters['cap_dim']:
            cap_ids, _ = pad_sequences(cap_ids,0)
            fd[self.cap_ids] = cap_ids
        att_ids, markable_length = pad_sequences(att_ids, [0, 0],max_len=self.parameters['max_len'])
        fd[self.att_ids] = att_ids
        fd[self.markable_lengths] = markable_length
        if self.parameters['len_dim']:
            len_ids = [e[1]-s[1] for s, e in zip(start_ids, end_ids)]
            fd[self.len_ids] = len_ids

        fd[self.start_ids] = start_ids
        fd[self.end_ids] = end_ids
        fd[self.gold_labels] = gold_labels

        fd[self.dropout] = dropout
        fd[self.hidden_dropout] = hidden_dropout

        return fd, sent_lenghts


    def get_feed_dict_batch(self, dataset, isTrain=True, singletons=None):
        """
        Take sentence data and return an input for
        the training or the evaluation function.
        """
        batch_size = self.parameters['b_size']
        word_ids, char_ids, cap_ids, start_ids, end_ids, gold_labels, att_ids, candidate_lengths, marks \
            = [], [], [], [], [], [], [], [], []
        dropout = self.parameters['dropout'] if isTrain else 1.0
        hidden_dropout = self.parameters['hidden_dropout'] if isTrain else 1.0
        for data in dataset:
            if len(word_ids) == batch_size:
                fd, sent_lenghts = self._pad_feed_dict(word_ids,
                                          char_ids, cap_ids,
                                          start_ids, end_ids,
                                          gold_labels, att_ids, dropout, hidden_dropout)
                yield fd, candidate_lengths, sent_lenghts, marks
                word_ids, char_ids, cap_ids, start_ids, end_ids, gold_labels, att_ids, candidate_lengths, marks \
                    = [], [], [], [], [], [], [], [], []
            ind = len(word_ids)
            words = data['words']
            chars = data['chars']
            if singletons is not None:
                words = insert_singletons(words, singletons)

            word_ids.append(words)
            char_ids.append(chars)
            cap_ids.append(data['caps'])

            keepall = not isTrain
            start, end, tags, heads, atts, mark = select_candidate(data,ind, keepall)
            start_ids.extend(start)
            end_ids.extend(end)
            att_ids.extend(atts)
            candidate_lengths.append(len(tags))
            marks.append(mark)
            if self.parameters['mode'] == MODE_HEAD:
                gold_labels.extend(heads)
            else:
                gold_labels.extend(tags)


        if len(word_ids) != 0:
            fd, sent_lenghts = self._pad_feed_dict(word_ids,
                                      char_ids, cap_ids,
                                      start_ids, end_ids,
                                      gold_labels, att_ids, dropout, hidden_dropout)
            yield fd, candidate_lengths, sent_lenghts, marks

    def train(self, train, dev, test, singletons=None):
        """Performs training with early stopping and lr exponential decay

        Args:
            train: dataset that yields tuple of (sentences, tags)
            dev: dataset

        """
        curr_time = time.time()
        best_dev_p, best_dev_r, best_dev_fb1, best_dev, best_dev_th = 0,0, 0, 0, 0.
        best_test_p, best_test_r, best_test_fb1, best_test, best_test_th = 0,0, 0, 0, 0.
        model_test_p, model_test_r, model_test_fb1, model_test = 0, 0, 0,0
        nepoch_no_imprv = 0 # for early stopping
        nepochs = self.parameters['nepochs']
        beta = self.parameters['f_beta']
        for epoch in range(nepochs):
            epoch_costs = []
            logging.info("Starting epoch %d out of %d" % (epoch + 1, nepochs))
            for i, (fd, _, _,_) in enumerate(self.get_feed_dict_batch(train,singletons=singletons)):
                _, batch_cost = self.sess.run(
                    [self.train_op, self.cost], feed_dict=fd
                )
                epoch_costs.append(batch_cost)

            logging.info("Average epoch cost:%f" % (sum(epoch_costs)/len(epoch_costs)))
            logging.info("Test on dev set:")
            dev_p, dev_r, dev_fb1,dev_beta, dev_th,  _,_ = self.eval(dev, self.thresholds)
            logging.info("Test on test set:")
            test_p, test_r, test_fb1, test_beta, test_th, _,_ = self.eval(test, [dev_th])
            #the threshold is not tuned on test set

            # early stopping and saving best parameters
            if best_dev == 0 or dev_beta > best_dev:
                nepoch_no_imprv = 0
                best_dev_p, best_dev_r, best_dev_fb1, best_dev, best_dev_th = dev_p, dev_r, dev_fb1,dev_beta, dev_th
                model_test_p, model_test_r, model_test_fb1, model_test = test_p, test_r, test_fb1, test_beta
                logging.info("New best FB%.2f on dev: %.2f%%." % (beta, dev_beta * 100))
                logging.info("Saving model on disk...")
                self.save()
                self.parameters['best_threshold'] = best_dev_th
                self.save_parameters()
            else:
                nepoch_no_imprv += 1


            if test_beta >= best_test:
                best_test_p, best_test_r, best_test_fb1, best_test, best_test_th = test_p, test_r, test_fb1, test_beta,test_th
                logging.info( "New best FB%.2f on test: %.2f%%." % (beta, test_beta * 100))

            logging.info( "Time used for epoch %d: %s" % (epoch+1, time_used(curr_time)))
            curr_time = time.time()


        logging.info("\n\nFinally results:")

        message = ('%-12s  precision: %.2f%%, recall: %.2f%%, FB1: %.2f, FB%.2f:  %.2f, Threshold: %.2f\n\n' % \
              ('Best dev', best_dev_p * 100, best_dev_r * 100, best_dev_fb1 * 100, beta, best_dev * 100, best_dev_th))
        message += ('%-12s  precision: %.2f%%, recall: %.2f%%, FB1: %.2f, FB%.2f:  %.2f, Threshold: %.2f\n\n' % \
              ('Best Test', best_test_p * 100, best_test_r * 100, best_test_fb1 * 100,beta, best_test * 100, best_test_th))
        message += ('%-12s  precision: %.2f%%, recall: %.2f%%, FB1: %.2f, FB%.2f:  %.2f\n\n' % \
         ('Model Test', model_test_p * 100, model_test_r * 100, model_test_fb1 * 100, beta, model_test * 100))

        logging.info(message)
        logging.info( "Finished.")


        return best_dev, message


    def eval(self, test, thresholds=[]):
        mode = self.parameters['mode']
        if mode == MODE_MENTION:
            return self.eval_mention(test,thresholds)
        elif mode == MODE_NER:
            return self.eval_ner(test)
        else:
            return self.eval_head(test)

    def eval_mention(self, test, thresholds=[]):
        """
        Evaluates performance on test set
        """
        o_id = self.o_id
        m_id = 0
        for i,s in self.id_to_tag.items():
            if s == 'M':
                m_id = i

        beta = self.parameters['f_beta']
        thresholds = [self.parameters['best_threshold']] if len(thresholds) == 0 else thresholds
        th_len = len(thresholds)
        true_positive, false_positive, false_negative, len_mention \
            = [0 for _ in xrange(th_len)], \
              [0 for _ in xrange(th_len)], \
              [0 for _ in xrange(th_len)],\
              [0 for _ in xrange(th_len)]

        preds = [[] for t in xrange(th_len)]
        for fd, cand_lengths, sent_lengths, marks in self.get_feed_dict_batch(test,False):
            scores, y_args = self.sess.run([self.scores, self.preds], feed_dict=fd)
            start_ids = fd[self.start_ids]
            end_ids = fd[self.end_ids]
            curr_index = 0
            for c_len, s_len, actual in zip(cand_lengths,sent_lengths, marks):
                sind,eind = curr_index, curr_index+c_len
                curr_index+=c_len
                score, y_tmp, start, end = \
                    scores[sind:eind], y_args[sind:eind],\
                    start_ids[sind:eind],end_ids[sind:eind]
                for t, threshold in enumerate(thresholds):
                    y_preds = []
                    for i in xrange(c_len):
                        #if y_tmp[i] != o_id and score[i, y_tmp[i]] >= threshold:
                        if score[i,m_id] >= threshold:
                            y_preds.append((start[i][1], end[i][1], score[i, y_tmp[i]]))

                    estimate = get_nested_mentions(y_preds)
                    out_estimate = [(b,e, 0,0) for (b,e) in estimate]
                    preds[t].append(sorted(out_estimate))

                    estimate = set(estimate)
                    actual = set(actual)



                    true_positive[t] += len([(b, e) for (b, e) in estimate & actual])
                    false_positive[t] += len([(b, e) for (b, e) in estimate - actual])
                    false_negative[t] += len([(b, e) for (b, e) in actual - estimate])
                    len_mention[t] += len([(b, e) for (b, e) in actual])

        best_t = 0
        best_fb = 0
        for t, th in enumerate(thresholds):
            tp, fp, fn = \
            true_positive[t], false_positive[t], false_negative[t]
            if tp !=0:
                p = float(tp) / float(tp + fp)
                r = float(tp) / float(tp + fn)
                fb = (1 + beta * beta) * p * r / (beta * beta * p + r)
            else:
                fb = 0.0
            if fb > best_fb:
                best_fb = fb
                best_t = t

        logging.info("With best threshold: %f" % thresholds[best_t])

        true_positive, false_positive, false_negative, len_mention = \
            true_positive[best_t], false_positive[best_t], false_negative[best_t], len_mention[best_t]
        if true_positive != 0:
            precision = float(true_positive) / float(true_positive + false_positive)
            recall = float(true_positive) / float(true_positive + false_negative)
            fb1 = 2.0 * precision * recall / (precision + recall)
            f_beta = (1+beta*beta) * precision * recall/ (beta*beta*precision + recall)
        else:
            precision, recall, fb1, f_beta = 0.0, 0.0, 0.0,0.0


        logging.info('%-12s  precision: %.2f%%, recall: %.2f%%, FB1: %.2f, FB%.2f:  %.2f %d' % \
              ('Mention', precision * 100, recall * 100, fb1 * 100,beta, f_beta*100, len_mention))

        return precision, recall, fb1, f_beta, thresholds[best_t], preds[best_t],None

    def eval_ner(self, test):
        """
        Evaluates performance on test set
        """

        preds = []
        corr, count = 0, 0
        for fd, cand_lengths, sent_lengths, marks in self.get_feed_dict_batch(test,False):
            y_args = self.sess.run(self.preds, feed_dict=fd)
            start_ids = fd[self.start_ids]
            end_ids = fd[self.end_ids]

            curr_index = 0
            for c_len, s_len, actual in zip(cand_lengths, sent_lengths, marks):
                sind, eind = curr_index, curr_index + c_len
                curr_index += c_len
                y_tmp, start, end = \
                    y_args[sind:eind], \
                    start_ids[sind:eind], end_ids[sind:eind]
                estimate = []
                for b, e, y,  m in zip(start, end,y_tmp, actual):
                    b,e = b[1],e[1]
                    assert b == m[0] and e == m[1]
                    if y == m[2]:
                        corr+=1
                    count +=1
                    estimate.append((b,e,y,0))
                preds.append(sorted(estimate))

        accuracy = float(corr) / float(count)
        logging.info('NER accuracy %.2f %d' % \
              (accuracy * 100, count))

        return accuracy, accuracy, accuracy, accuracy, 0, preds,None

    def eval_head(self, test):
        """
        Evaluates performance on test set
        """
        preds = []
        head_scores = []
        corr, count = 0, 0
        for fd, cand_lengths, sent_lengths, marks in self.get_feed_dict_batch(test, False):
            y_args,h_score = self.sess.run([self.preds,self.head_socres], feed_dict=fd)
            start_ids = fd[self.start_ids]
            end_ids = fd[self.end_ids]

            curr_index = 0
            for c_len, s_len, actual in zip(cand_lengths, sent_lengths, marks):
                sind, eind = curr_index, curr_index + c_len
                curr_index += c_len
                y_tmp, start, end = \
                    y_args[sind:eind], \
                    start_ids[sind:eind], end_ids[sind:eind]
                estimate = []
                for b, e, y, m in zip(start, end, y_tmp, actual):
                    b,e = b[1],e[1]
                    assert b == m[0] and e == m[1]
                    if y+b == m[3]:
                        corr += 1
                    count += 1
                    estimate.append((b, e, m[2], y+b))
                preds.append(sorted(estimate))
            head_scores.extend(h_score)
        accuracy = float(corr) / float(count)
        logging.info('Head accuracy %.2f %d' % \
                     (accuracy * 100, count))

        return accuracy, accuracy, accuracy, accuracy, 0, preds,head_scores