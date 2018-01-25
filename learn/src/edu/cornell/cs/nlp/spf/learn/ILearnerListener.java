package edu.cornell.cs.nlp.spf.learn;

import edu.cornell.cs.nlp.spf.data.IDataItem;

public interface ILearnerListener {

    default void beganDataItem(IDataItem<?> dataItem) {

    }

    default void finishedDataItem(IDataItem<?> dataItem) {

    }

}
