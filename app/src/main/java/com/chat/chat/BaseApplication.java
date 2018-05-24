package com.chat.chat;

import android.arch.lifecycle.MutableLiveData;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerOption;

public class BaseApplication extends MultiDexApplication {
    private static final String API_KEY = "b4a52f5f-fc52-4939-9c9b-c2adaf9fe043";
    private static final String DOMAIN = "localhost";

    private Peer _peer;
    private String usedId;

    private MutableLiveData<String> usedIdLiveData = new MutableLiveData<>();

    @Override
    public void onCreate() {
        super.onCreate();
        initPeer();
    }

    private void initPeer() {
        //
        // Initialize Peer
        //
        PeerOption option = new PeerOption();
        option.key = API_KEY;
        option.domain = DOMAIN;
        _peer = new Peer(this, option);

        //
        // Get PeerId
        //
        _peer.on(Peer.PeerEventEnum.OPEN, object -> {
            // Show my ID
            usedId = (String) object;
            usedIdLiveData.postValue(usedId);
            Log.d("TAG", "onCallback: " + usedId);
        });
    }

    public Peer get_peer() {
        return _peer;
    }

    public String getUsedId() {
        return usedId;
    }

    public MutableLiveData<String> getUsedIdLiveData() {
        return usedIdLiveData;
    }

    public void setUsedIdLiveData(MutableLiveData<String> usedIdLiveData) {
        this.usedIdLiveData = usedIdLiveData;
    }
}