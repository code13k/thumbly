package org.code13k.thumbly.business;

import com.hazelcast.core.*;
import org.code13k.thumbly.app.Cluster;
import org.code13k.thumbly.model.Procedure;
import org.code13k.thumbly.web.client.CachedWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteredProcedure {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(ClusteredProcedure.class);

    // Const
    private static final String TOPIC = "Code13k-Thumbly-Clustered-Procedure-Topic";
    private static final String PROCEDURE_METHOD_DELETE_CACHE = "DELETE_CACHE";

    // Data
    private ITopic<Procedure> mTopic = null;


    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final ClusteredProcedure INSTANCE = new ClusteredProcedure();
    }

    public static ClusteredProcedure getInstance() {
        return ClusteredProcedure.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private ClusteredProcedure() {
        mLogger.trace("ClusteredProcedure()");
    }

    /**
     * Initialize
     */
    synchronized public void init() {
        if (mTopic == null) {
            mTopic = Cluster.getInstance().getHazelcastInstance().getTopic(TOPIC);
            mTopic.addMessageListener(new MessageListener<Procedure>() {
                @Override
                public void onMessage(Message<Procedure> message) {
                    mLogger.trace("onMessage() : " + message.getMessageObject());
                    Procedure procedure = message.getMessageObject();

                    // PROCEDURE_DELETE_CACHE
                    if (PROCEDURE_METHOD_DELETE_CACHE.equals(procedure.getMethod()) == true) {
                        String url = (String) procedure.getParams();
                        CachedWebClient.getInstance().deleteCache(url);
                    }
                }
            });
        } else {
            mLogger.info("Duplicated initializing");
        }
    }

    /**
     * Delete cache
     */
    public void deleteCache(String url) {
        Procedure procedure = new Procedure();
        procedure.setId();
        procedure.setMethod(PROCEDURE_METHOD_DELETE_CACHE);
        procedure.setParams(url);
        mTopic.publish(procedure);
    }
}
