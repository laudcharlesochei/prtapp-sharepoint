package prt.springbootthymeleafcrudwebapp.model;

import java.io.Serializable;

/**
 * An employee record, stored as one item in the SharePoint Online list
 * "employee" on https://dmail.sharepoint.com/sites/MentorMeetingSystem.
 *
 * No longer a JPA entity: {@code id} is the SharePoint list item ID
 * (a positive integer assigned by SharePoint; 0 means "not saved yet").
 *
 * @author bblns18
 */
public class Employee implements Serializable {

    private long id;
    private String firstName;
    private String lastName;
    private String email;

    public Employee() {
    }

    public Employee(long id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
