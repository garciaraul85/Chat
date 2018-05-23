package com.chat.chat.view;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.chat.chat.R;
import com.chat.chat.model.Contact;

import java.util.ArrayList;
import java.util.List;

public class AccessContactsActivity extends AppCompatActivity {
    ListView listView;
    private static CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_contacts);
        new FetchContactsTask().execute();
    }

    private void loadContacts(final ArrayList<Contact> contacts) {
        listView = (ListView)findViewById(R.id.list);
        adapter = new CustomAdapter(contacts,getApplicationContext());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Contact dataModel= contacts.get(position);

                new SendInvitationTask().execute(dataModel.getEmail());
            }
        });
    }

    private class FetchContactsTask extends AsyncTask<Void, Void, ArrayList<Contact>> {

        private static final String TAG = "FetchContactsTask";
        private final String DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;

        private final String FILTER = DISPLAY_NAME + " NOT LIKE '%@%'";

        private final String ORDER = String.format("%1$s COLLATE NOCASE", DISPLAY_NAME);

        @SuppressLint("InlinedApi")
        private final String[] PROJECTION = {
                ContactsContract.Contacts._ID,
                DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        };

        @Override
        protected ArrayList<Contact> doInBackground(Void... params) {
            try {
                ArrayList<Contact> contacts = new ArrayList<>();

                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, PROJECTION, FILTER, null, ORDER);
                if (cursor != null && cursor.moveToFirst()) {

                    do {
                        // get the contact's information
                        String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                        Integer hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        // get the user's email address
                        String email = null;
                        Cursor ce = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null);
                        if (ce != null && ce.moveToFirst()) {
                            email = ce.getString(ce.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                            ce.close();
                        }

                        // get the user's phone number
                        String phone = null;
                        if (hasPhone > 0) {
                            Cursor cp = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                            if (cp != null && cp.moveToFirst()) {
                                phone = cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                cp.close();
                            }
                        }

                        // if the user user has an email or phone then add it to contacts
                        if ((!TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                && !email.equalsIgnoreCase(name)) || (!TextUtils.isEmpty(phone))) {
                            Contact contact = new Contact(name, email, null);
                            contacts.add(contact);
                        }

                    } while (cursor.moveToNext());

                    // clean up cursor
                    cursor.close();
                }
                return contacts;
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Contact> contacts) {
            if (contacts != null) {
                // success
                loadContacts(contacts);
            } else {
                // show failure
                // syncFailed();
            }
        }
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