package com.t2308e.entity;

import com.t2308e.annotations.MyColumn;
import com.t2308e.annotations.MyEntity;
import com.t2308e.annotations.MyId;
import com.t2308e.annotations.MyTransient;

@MyEntity(tableName = "users") // Hoặc @MyEntity, lúc đó tên bảng sẽ là "user" hoặc "users"
public class User {

    @MyId
    private Long id; // Giả sử ID là auto-increment

    @MyColumn(name = "user_name")
    private String userName;

    @MyColumn // Sẽ lấy tên cột là "email"
    private String email;

    private int age; // Mặc định sẽ được map vào cột "age"

    @MyTransient
    private String temporaryPassword;

    public User() { // Cần constructor không tham số
    }

    public User(String userName, String email, int age) {
        this.userName = userName;
        this.email = email;
        this.age = age;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                '}';
    }
}
