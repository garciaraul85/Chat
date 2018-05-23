package com.chat.chat.view;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chat.chat.BaseApplication;
import com.chat.chat.R;
import com.chat.chat.model.ChatMessage;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MainActivity extends AppCompatActivity implements LifecycleOwner {

    private static final int SIGN_IN_REQUEST_CODE = 1;
    private static final int SELECT_FILE = 3;

    private FirebaseListAdapter<ChatMessage> adapter;

    private StorageReference mStorageRef;

    private String userName;
    private HashMap contacts;

    private LifecycleRegistry mLifecycleRegistry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);

        setContentView(R.layout.activity_main);

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

        ListView listOfMessages = (ListView) findViewById(R.id.rvChat);

        adapter = new FirebaseListAdapter<ChatMessage>(this, ChatMessage.class,
                R.layout.message, FirebaseDatabase.getInstance().getReference()) {
            @Override
            protected void populateView(View v, ChatMessage model, int position) {
                // Get references to the views of message.xml
                TextView messageText = v.findViewById(R.id.message_text);
                TextView messageUser = v.findViewById(R.id.message_user);
                TextView messageTime = v.findViewById(R.id.message_time);
                TextView sharedLink  = v.findViewById(R.id.file_url);

                if (model.getMessageText() != null) {
                    // Set their text
                    boolean isValid = URLUtil.isValidUrl(model.getMessageText());
                    if (isValid) {
                        messageText.setText(R.string.download_share_file);
                        sharedLink.setText(model.getMessageText());
                    } else {
                        messageText.setText(model.getMessageText());
                    }
                    messageUser.setText(model.getMessageUser());

                    // Format the date before showing it
                    messageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
                            model.getMessageTime()));
                } else {
                    if (!model.getMessageUser().equals(userName) && model.getIdUser() != null) {
                        contacts.put(model.getMessageUser(), model.getIdUser());
                    }
                }


            }
        };

        listOfMessages.setAdapter(adapter);

        listOfMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                boolean isValid = URLUtil.isValidUrl(adapter.getItem(position).getMessageText());
                if (isValid) {
                    Intent browserIntent = new Intent(
                            Intent.ACTION_VIEW, Uri.parse(adapter.getItem(position).getMessageText()));
                    startActivity(browserIntent);
                }
            }
        });
    }

    private void postMessage() {
        FloatingActionButton fab =
                (FloatingActionButton)findViewById(R.id.fab);

        final String usedId = ((BaseApplication) getApplication()).getUsedId();

        Log.d("TAG", "postMessage: " + usedId);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = (EditText)findViewById(R.id.input);

                // Read the input field and push a new instance
                // of ChatMessage to the Firebase database
                FirebaseDatabase.getInstance()
                        .getReference()
                        .push()
                        .setValue(new ChatMessage(
                                input.getText().toString(),
                                userName, usedId));

                // Clear the input
                input.setText("");
            }
        });
    }

    private void postUserId() {
        final Observer<String> userIdObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String userId) {
                if (userId != null) {
                    FirebaseDatabase.getInstance()
                            .getReference()
                            .push()
                            .setValue(new ChatMessage(
                                    userName, userId));
                }
            }
        };

        ((BaseApplication) getApplication()).getUsedIdLiveData().observe(this, userIdObserver);

    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }
}