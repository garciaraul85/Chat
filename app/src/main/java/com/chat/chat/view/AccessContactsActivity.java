package com.chat.chat.view;

import android.Manifest;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import com.chat.chat.R;
import com.chat.chat.model.Contact;
import com.chat.chat.viewmodel.ContactsViewModel;

import java.util.ArrayList;

public class AccessContactsActivity extends AppCompatActivity implements LifecycleOwner {
    private ListView listView;
    private static CustomAdapter adapter;
    private ContactsViewModel contactsViewModel;
    private LifecycleRegistry mLifecycleRegistry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_contacts);

        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);

        contactsViewModel = ViewModelProviders.of(this).get(ContactsViewModel.class);
        readContacts();
    }

    private void readContacts() {


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            contactsViewModel.getContactsLiveData().observe(this, contacts -> {
                if (contacts != null) {
                    loadContacts(contacts);
                }
            });
            contactsViewModel.getFetchContactsTask().execute(this);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                    Toast.makeText(this, "Read contacts permission is required to function app correctly", Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            1);
                }
            }
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        mLifecycleRegistry.markState(Lifecycle.State.STARTED);
    }

    private void loadContacts(final ArrayList<Contact> contacts) {
        listView = (ListView)findViewById(R.id.list);
        adapter = new CustomAdapter(contacts, getApplicationContext());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {

            Contact dataModel= contacts.get(position);

            contactsViewModel.getEmailLiveData().observe(this, email -> sendInvitation(email));

            contactsViewModel.getSendInvitationTask().execute(dataModel.getEmail());
            //new SendInvitationTask().execute(dataModel.getEmail());
        });
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }


    private class SendInvitationTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... email) {
            sendInvitation(email[0]);
            return null;
        }
    }

    private void sendInvitation(String email) {

        ShareCompat.IntentBuilder sharingIntent = ShareCompat.IntentBuilder.
                from(this);
        sharingIntent.setType("text/plain");
        sharingIntent.addEmailTo(email);
        sharingIntent.setSubject(getString(R.string.invitation_subject));
        sharingIntent.setText(getString(R.string.invitation_body));
        startActivity(sharingIntent.createChooserIntent());

    }

}