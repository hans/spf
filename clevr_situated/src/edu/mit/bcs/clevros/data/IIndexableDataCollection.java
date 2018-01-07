package edu.mit.bcs.clevros.data;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;

public interface IIndexableDataCollection<DI extends IDataItem<?>> extends IDataCollection<DI> {

    DI get(int idx);

}
