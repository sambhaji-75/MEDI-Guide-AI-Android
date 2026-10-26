package com.mediguide.ai;

public class UserModel {
    private String userId, name, gender, preferredLanguage, primaryCareNeed;
    private int age;

    public UserModel() {}

    public UserModel(String userId, String name, int age, String gender,
                     String preferredLanguage, String primaryCareNeed) {
        this.userId = userId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.preferredLanguage = preferredLanguage;
        this.primaryCareNeed = primaryCareNeed;
    }

    public String getUserId() { return userId; }
    public void setUserId(String v) { userId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public int getAge() { return age; }
    public void setAge(int v) { age = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { gender = v; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String v) { preferredLanguage = v; }
    public String getPrimaryCareNeed() { return primaryCareNeed; }
    public void setPrimaryCareNeed(String v) { primaryCareNeed = v; }
}
