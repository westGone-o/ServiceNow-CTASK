package com.servicenow.ctaskcreator.model;

public class AppSettings {
    private String snowId;
    private String snowPassword;
    private String excelPath;
    private String assignmentGroup;
    private String assignedPerson;

    public String getSnowId() {
        return this.snowId;
    }

    public void setSnowId(String snowId) {
        this.snowId = snowId;
    }

    public String getSnowPassword() {
        return this.snowPassword;
    }

    public void setSnowPassword(String snowPassword) {
        this.snowPassword = snowPassword;
    }

    public String getExcelPath() {
        return this.excelPath;
    }

    public void setExcelPath(String excelPath) {
        this.excelPath = excelPath;
    }

    public String getAssignmentGroup() {
        return this.assignmentGroup;
    }

    public void setAssignmentGroup(String assignmentGroup) {
        this.assignmentGroup = assignmentGroup;
    }

    public String getAssignedPerson() {
        return this.assignedPerson;
    }

    public void setAssignedPerson(String assignedPerson) {
        this.assignedPerson = assignedPerson;
    }
}
