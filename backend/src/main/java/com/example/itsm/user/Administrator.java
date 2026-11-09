package com.example.itsm.user;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    protected Administrator() {
    }

    public Administrator(String name, String email, String passwordHash, String phone, String jobTitle) {
        super(name, email, passwordHash, phone);
        this.jobTitle = jobTitle;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobTitle() {
        return jobTitle;
    }
}
