package com.chat.chat.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.util.Log;

import com.chat.chat.BaseApplication;
import com.chat.chat.model.ChatMessage;
import com.chat.chat.view.MainActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ContactsChatViewModel extends AndroidViewModel {

    private MutableLiveData<HashMap<String, String>> contactsLiveData = new MutableLiveData<>();
    private DatabaseReference mFirebaseRef;

    public ContactsChatViewModel(@NonNull Application application) {
        super(application);
        mFirebaseRef = FirebaseDatabase.getInstance().getReference("userId");
    }

    public void fetchContactsChatIds(String userName) {
        HashMap contacts = new LinkedHashMap<String, String>();
        mFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    try {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            ChatMessage model = ds.getValue(ChatMessage.class);
                            if (!model.isMessage() && !model.getMessageUser().equals(userName) && model.getIdUser() != null) {
                                Log.d("tag", "_www add contact " + model.getIdUser());
                                contacts.put(model.getMessageUser(), model.getIdUser());
                                contactsLiveData.postValue(contacts);
                            }
                        }
                    } catch (Exception ex) {
                        Log.e("TAG", ex.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TAG", "onCancelled: ", databaseError.toException());
                contactsLiveData.postValue(contacts);
            }
        });
    }

    public MutableLiveData<HashMap<String, String>> getContactsLiveData() {
        return contactsLiveData;
    }

    public void postUserId(MainActivity activity, String userName) {
        Query applesQuery = mFirebaseRef.orderByChild("messageUser").equalTo(userName);

        applesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot appleSnapshot: dataSnapshot.getChildren()) {
                    Log.d("tag", "_rrr remove value");
                    appleSnapshot.getRef().removeValue();
                }
                final Observer<String> userIdObserver = userId -> {
                    if (userId != null) {
                        Log.d("tag", "_rrr add value " + userId);
                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("userId")
                                .push()
                                .setValue(new ChatMessage(
                                        userName, userId));
                    }
                };

                ((BaseApplication) getApplication()).getUsedIdLiveData().observe(activity, userIdObserver);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TAG", "onCancelled", databaseError.toException());
            }
        });


    }

}