package com.itsm.itsm.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    private String department;

    protected Employee() {
    }

    public Employee(String name, String email, String passwordHash, String department) {
        super(name, email, passwordHash);
        this.department = department;
    }

    @Override
    public Role getRole() {
        return Role.EMPLOYEE;
    }

    // The admin corrects an employee's name or department
    public void updateDetails(String name, String department) {
        changeName(name);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }
}
