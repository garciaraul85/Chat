package com.chat.chat.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.chat.chat.model.ChatMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private MutableLiveData<List<ChatMessage>> chatMessageListLiveData = new MutableLiveData<>();
    private DatabaseReference mFirebaseRef;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        mFirebaseRef = FirebaseDatabase.getInstance().getReference("message");
    }

    public void fetchChatMessages() {
        List<ChatMessage> chatMessageList = new ArrayList<>();
        Log.d("TAG", "_xxx fetchChatMessages: ");
        mFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("TAG", "_xxx onDataChange1: ");
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    Log.d("TAG", "_xxx onDataChange2: ");
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        ChatMessage model = ds.getValue(ChatMessage.class);
                        if (model.isMessage()) {
                            chatMessageList.add(model);
                        }
                    }
                    chatMessageListLiveData.postValue(chatMessageList);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TAG", "_xxx onCancelled: ", databaseError.toException());
                chatMessageListLiveData.postValue(chatMessageList);
            }
        });
    }

    public LiveData<List<ChatMessage>> getChatMessageListLiveData() {
        return chatMessageListLiveData;
    }

    public void postMessage(String message, String userName, String usedId) {
        // Read the input field and push a new instance
        // of ChatMessage to the Firebase database
        FirebaseDatabase.getInstance()
                .getReference()
                .child("message")
                .push()
                .setValue(new ChatMessage(message, userName, usedId));
    }
}