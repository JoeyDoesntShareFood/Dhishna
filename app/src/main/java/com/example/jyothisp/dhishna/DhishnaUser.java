package com.example.jyothisp.dhishna;

public class DhishnaUser {
    private String Name, Email, Phone;
    private String Gender; // M=male, F=female.
    private String Institute;

    public DhishnaUser(String name, String email, String phone, String gender, String institute){
        Name = name;
        Email = email;
        Phone = phone;
        Gender = gender;
        Institute = institute;
    }

    public String getName() {
        return Name;
    }

    public String getEmail() {
        return Email;
    }

    public String getPhone() {
        return Phone;
    }

    public String getGender() {
        return Gender;
    }

    public String getInstitute() {
        return Institute;
    }
}
