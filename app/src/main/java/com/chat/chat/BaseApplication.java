package com.chat.chat;

import android.app.Application;

import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerOption;

public class BaseApplication extends Application {
    private static final String API_KEY = "b4a52f5f-fc52-4939-9c9b-c2adaf9fe043";
    private static final String DOMAIN = "localhost";

    private Peer _peer;

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
    }

    public Peer get_peer() {
        return _peer;
    }
}