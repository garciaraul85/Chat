package com.chat.chat.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.chat.chat.model.Contact;
import com.chat.chat.view.AccessContactsActivity;

import java.util.ArrayList;

public class ContactsViewModel extends AndroidViewModel {
    private MutableLiveData<ArrayList<Contact>> contactsLiveData = new MutableLiveData<>();
    private FetchContactsTask fetchContactsTask = new FetchContactsTask();

    private MutableLiveData<String> emailLiveData = new MutableLiveData<>();
    private SendInvitationTask sendInvitationTask = new SendInvitationTask();


    public ContactsViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<ArrayList<Contact>> getContactsLiveData() {
        return contactsLiveData;
    }

    public FetchContactsTask getFetchContactsTask() {
        return fetchContactsTask;
    }

    public class FetchContactsTask extends AsyncTask<AccessContactsActivity, Void, ArrayList<Contact>> {

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
        protected ArrayList<Contact> doInBackground(AccessContactsActivity... params) {
            try {
                ArrayList<Contact> contacts = new ArrayList<>();

                ContentResolver cr = params[0].getContentResolver();
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
            contactsLiveData.postValue(contacts);
        }
    }

    public MutableLiveData<String> getEmailLiveData() {
        return emailLiveData;
    }

    public SendInvitationTask getSendInvitationTask() {
        return sendInvitationTask;
    }

    public class SendInvitationTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... email) {
            emailLiveData.postValue(email[0]);
            //sendInvitation(email[0]);
            return null;
        }
    }

}