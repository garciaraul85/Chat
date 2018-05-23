package com.chat.chat.model;

import com.orm.SugarRecord;

public class Contact extends SugarRecord {
    private String name;
    private String email;
    private String uId;

    public Contact() {

    }

    public Contact(String name, String email, String uId) {
        this.name = name;
        this.email = email;
        this.uId = uId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }
}
