package com.exxeta.correomqtt.business.dispatcher;

import com.exxeta.correomqtt.business.model.SubscriptionDTO;

public interface SubscribeGlobalObserver extends BaseConnectionObserver {
    void onSubscribedSucceeded(String connectionId, SubscriptionDTO subscriptionDTO);
    void onSubscribeRemoved(String connectionId, SubscriptionDTO subscriptionDTO);
    void onSubscribeCleared(String connectionId);
}
