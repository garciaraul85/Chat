package com.chat.chat.view;

import android.app.Activity;
import android.app.FragmentManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import com.chat.chat.BaseApplication;
import com.chat.chat.R;
import com.chat.chat.model.ChatMessage;
import com.chat.chat.viewmodel.ChatViewModel;
import com.chat.chat.viewmodel.ContactsChatViewModel;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;

public class MainActivity extends AppCompatActivity implements LifecycleOwner, ChatAdapter.OnItemClickListener {

    private static final int SIGN_IN_REQUEST_CODE = 1;
    private static final int SELECT_FILE = 3;

    private ChatViewModel chatViewModel;
    private ContactsChatViewModel contactsChatViewModel;

    private StorageReference mStorageRef;

    private String userName;
    private HashMap contacts;

    private ChatAdapter chatAdapter;
    private RecyclerView chatListRecycler;
    private List<ChatMessage> chatMessageList = new ArrayList<>();

    private LifecycleRegistry mLifecycleRegistry;
    private Activity activity;

    private Handler _handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;
        _handler = new Handler(Looper.getMainLooper());

        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);

        chatViewModel          = ViewModelProviders.of(this).get(ChatViewModel.class);
        contactsChatViewModel = ViewModelProviders.of(this).get(ContactsChatViewModel.class);

        setContentView(R.layout.activity_main);

        chatListRecycler = (RecyclerView) findViewById(R.id.rvChat);
        chatListRecycler.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatListRecycler.setLayoutManager(linearLayoutManager);

        contacts = new LinkedHashMap<String, String>();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Start sign in/sign up activity
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .build(),
                    SIGN_IN_REQUEST_CODE
            );
        } else {
            // User is already signed in. Therefore, display
            // a welcome Toast
            Toast.makeText(this,
                    "Welcome " + FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getDisplayName(),
                    Toast.LENGTH_LONG)
                    .show();

            // Load chat room contents
            displayChatMessages();
        }

        postMessage();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLifecycleRegistry.markState(Lifecycle.State.STARTED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final String usedId = ((BaseApplication) getApplication()).getUsedId();

        if (requestCode == SIGN_IN_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(this,
                        "Successfully signed in. Welcome!",
                        Toast.LENGTH_LONG)
                        .show();
                displayChatMessages();
            } else {
                Toast.makeText(this,
                        "We couldn't sign you in. Please try again later.",
                        Toast.LENGTH_LONG)
                        .show();

                // Close the app
                finish();
            }
        } else if (requestCode == SELECT_FILE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    try {
                        String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

                        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                        final StorageReference storageRef = firebaseStorage.getReferenceFromUrl("gs://chat-16ef7.appspot.com");

                        Uri file = Uri.fromFile(new File(filePath));
                        mStorageRef = storageRef.child(file.getLastPathSegment());
                        UploadTask uploadTask = mStorageRef.putFile(file);
                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                Log.i("TAG", exception.toString());
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                FirebaseDatabase.getInstance()
                                        .getReference()
                                        .push()
                                        .setValue(new ChatMessage(downloadUrl.toString(),
                                                FirebaseAuth.getInstance()
                                                        .getCurrentUser()
                                                        .getDisplayName(),
                                                usedId)
                                        );
                            }
                        });

                        Log.d("TAG", "onActivityResult: " + filePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this,
                                    "You have been signed out.",
                                    Toast.LENGTH_LONG)
                                    .show();

                            // Close activity
                            finish();
                        }
                    });
        } else if (item.getItemId() == R.id.menu_share) {
            processFile();
        } else if (item.getItemId() == R.id.menu_call) {
            showPeerIDs();
        } else {
            Intent intent = new Intent(this, AccessContactsActivity.class);
            startActivity(intent);
        }
        return true;
    }

    private void processFile() {

        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(3)
                .withFilterDirectories(true) // Set directories filterable (false by default)
                .withHiddenFiles(true) // Show hidden files and folders
                .start();

    }

    private void displayChatMessages() {

        this.userName = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getDisplayName();

        postUserId();

        chatAdapter = new ChatAdapter(this);
        chatListRecycler.setAdapter(chatAdapter);

        //ListView listOfMessages = (ListView) findViewById(R.id.rvChat);

        chatViewModel.getChatMessageListLiveData().observe(this, chatMessages -> {
            Log.d("TAG", "_xxx onChanged: ");
            chatAdapter.setItems(chatMessages);
            chatAdapter.notifyDataSetChanged();
        });
        chatViewModel.fetchChatMessages();

        contactsChatViewModel.getContactsLiveData().observe(this, contactsViewModel -> {
            contacts.clear();
            contacts.putAll(contactsViewModel);
        });
        contactsChatViewModel.fetchContactsChatIds(userName);
    }

    private void postMessage() {
        FloatingActionButton fab =
                (FloatingActionButton)findViewById(R.id.fab);

        final String usedId = ((BaseApplication) getApplication()).getUsedId();

        Log.d("TAG", "postMessage: " + usedId);

        fab.setOnClickListener(view -> {
            EditText input = (EditText)findViewById(R.id.input);

            chatViewModel.postMessage(
                    input.getText().toString(), userName, usedId);

            // Clear the input
            input.setText("");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        Query applesQuery = ref.child("userId").orderByChild("messageUser").equalTo(userName);

        applesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot appleSnapshot: dataSnapshot.getChildren()) {
                    Log.d("tag", "_rrr remove value");
                    appleSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TAG", "onCancelled", databaseError.toException());
            }
        });
    }

    private void postUserId() {
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

        ((BaseApplication) getApplication()).getUsedIdLiveData().observe((MainActivity) activity, userIdObserver);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    @Override
    public void onItemClick(ChatMessage item) {
        boolean isValid = URLUtil.isValidUrl(item.getMessageText());
        if (isValid) {
            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW, Uri.parse(item.getMessageText()));
            startActivity(browserIntent);
        }
    }

    //
    // Listing all peers
    //
    void showPeerIDs() {
        String usedId = ((BaseApplication) getApplication()).getUsedId();
        Peer _peer = ((BaseApplication) getApplication()).get_peer();

        if ((null == _peer) || (null == usedId) || (0 == usedId.length())) {
            Toast.makeText(this, "Your PeerID is null or invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all IDs connected to the server
        final Context fContext = this;
        _peer.listAllPeers(new OnCallback() {
            @Override
            public void onCallback(Object object) {
                if (!(object instanceof JSONArray)) {
                    return;
                }

                JSONArray peers = (JSONArray) object;
                ArrayList<String> _listPeerIds = new ArrayList<>();
                String peerId;

                // Exclude my own ID
                for (int i = 0; peers.length() > i; i++) {
                    try {
                        peerId = peers.getString(i);
                        if (!usedId.equals(peerId)) {
                            _listPeerIds.add(peerId);
                        }
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }

                // Show IDs using DialogFragment
                if (!contacts.isEmpty() && 0 < _listPeerIds.size()) {
                    FragmentManager mgr = getFragmentManager();
                    PeerListDialogFragment dialog = new PeerListDialogFragment();
                    dialog.setListener(
                            new PeerListDialogFragment.PeerListDialogFragmentListener() {
                                @Override
                                public void onItemClick(final String item) {
                                    _handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            onPeerSelected(item);
                                        }
                                    });
                                }
                            });

                    Iterator it = contacts.entrySet().iterator();
                    HashMap newContactList = new LinkedHashMap<String, String>();

                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        System.out.println(pair.getKey() + " = " + pair.getValue());
                        for (String peer : _listPeerIds) {
                            if (peer.equals(pair.getValue())) {
                                newContactList.put(pair.getKey(), pair.getValue());
                            }
                        }
                    }

                    dialog.setItems(newContactList);
                    dialog.show(mgr, "peerlist");
                } else {
                    Toast.makeText(fContext, "PeerID list (other than your ID) is empty.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void onPeerSelected(String item) {
        Log.d("tag", "Open new activity " + item);
        Intent intent = new Intent(getBaseContext(), VideoActivity.class);
        intent.putExtra("strPeerId", item);
        startActivity(intent);
    }
}