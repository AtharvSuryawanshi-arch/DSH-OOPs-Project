package com.example.itsm.user;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    @Column(length = 120)
    private String department;

    protected Employee() {
    }

    public Employee(String name, String email, String passwordHash, String phone, String department) {
        super(name, email, passwordHash, phone);
        this.department = department;
    }

    @Override
    public Role getRole() {
        return Role.EMPLOYEE;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }
}
