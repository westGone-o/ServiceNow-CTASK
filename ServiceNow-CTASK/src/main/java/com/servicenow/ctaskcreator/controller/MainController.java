package com.servicenow.ctaskcreator.controller;

import com.servicenow.ctaskcreator.model.AppSettings;
import com.servicenow.ctaskcreator.service.ExcelReader;
import com.servicenow.ctaskcreator.service.ServiceNowClient;
import com.servicenow.ctaskcreator.service.SettingsManager;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class MainController {
    @FXML
    private TextField txtExcelPath;
    @FXML
    private TextField txtAssignmentGroup;
    @FXML
    private TextField txtAssignedPerson;
    @FXML
    private TextField txtCrNumber;
    @FXML
    private TextArea txtDetailedDescription;
    @FXML
    private TextArea txtLog;
    @FXML
    private CheckBox chkAnalysis;
    @FXML
    private CheckBox chkDesign;
    @FXML
    private CheckBox chkDevelopment;
    @FXML
    private CheckBox chkTest;
    @FXML
    private CheckBox chkTAS;
    @FXML
    private CheckBox chkDocumentation;
    @FXML
    private CheckBox chkDeployment;
    private ServiceNowClient snowClient;
    
    // Store CR information
    private String crSysId = "";
    private String crShortDesc = "";

    @FXML
    public void initialize() {
        this.log("Application started.");
        AppSettings settings = SettingsManager.loadSettings();
        this.txtExcelPath.setText(settings.getExcelPath() != null ? settings.getExcelPath() : "");
        this.txtAssignmentGroup.setText(settings.getAssignmentGroup() != null ? settings.getAssignmentGroup() : "");
        this.txtAssignedPerson.setText(settings.getAssignedPerson() != null ? settings.getAssignedPerson() : "");
        if (settings.getSnowId() != null && !settings.getSnowId().isEmpty()) {
            this.log("Settings loaded successfully.");
        }
    }
    
    // Receive ServiceNowClient from LoginController
    public void setServiceNowClient(ServiceNowClient client) {
        this.snowClient = client;
        this.log("ServiceNow connected successfully.");
    }

    @FXML
    public void onSelectExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", new String[]{"*.xlsx", "*.xls"}));
        File file = fileChooser.showOpenDialog(this.txtExcelPath.getScene().getWindow());
        if (file != null) {
            this.txtExcelPath.setText(file.getAbsolutePath());
            this.log("Excel file selected: " + file.getName());
            this.saveCurrentSettings();
        }
    }
    
    // Fetch CR information
    @FXML
    public void onFetchCR() {
        if (this.snowClient == null) {
            this.log("[Error] Please connect to ServiceNow first.");
            return;
        }

        String crNumber = this.txtCrNumber.getText().trim();
        if (crNumber.isEmpty()) {
            this.log("[Error] Please enter CR number.");
            return;
        }

        this.log("Fetching CR information: " + crNumber);

        new Thread(() -> {
            try {
                Map<String, String> crDetails = this.snowClient.getCRDetails(crNumber);
                if (crDetails.isEmpty() || !crDetails.containsKey("sys_id")) {
                    Platform.runLater(() -> this.log("[Error] CR not found: " + crNumber));
                    return;
                }

                this.crSysId = crDetails.get("sys_id");
                this.crShortDesc = crDetails.get("short_description");
                String crDescription = crDetails.get("description");

                Platform.runLater(() -> {
                    this.txtDetailedDescription.setText(crDescription);
                    this.log("CR information retrieved successfully.");
                    this.log("   - Short Description: " + this.crShortDesc);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    this.log("[Error] CR lookup failed: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    public void onCreateTasks() {
        if (this.snowClient == null) {
            this.log("[Error] Please connect to ServiceNow first.");
            return;
        }

        // Check if CR information has been fetched
        if (this.crSysId.isEmpty()) {
            this.log("[Error] Please fetch CR information first. (Click Fetch button)");
            return;
        }

        String assignmentGroup = this.txtAssignmentGroup.getText().trim();
        String assignedPerson = this.txtAssignedPerson.getText().trim();
        String excelPath = this.txtExcelPath.getText().trim();
        String crDescription = this.txtDetailedDescription.getText().trim();

        if (assignmentGroup.isEmpty()) {
            this.log("[Error] Please enter Assignment Group.");
            return;
        }
        if (assignedPerson.isEmpty()) {
            this.log("[Error] Please enter Assigned Person.");
            return;
        }

        this.saveCurrentSettings();

        new Thread(() -> {
            try {
                Map<String, Double> efforts;
                if (excelPath.isEmpty()) {
                    this.log("[Warning] No Excel file - effort will be set to 0");
                    efforts = new HashMap<>();
                    efforts.put("Analysis", 0.0);
                    efforts.put("Design", 0.0);
                    efforts.put("Development", 0.0);
                    efforts.put("TEST", 0.0);
                    efforts.put("TAS", 0.0);
                    efforts.put("Documentation", 0.0);
                    efforts.put("Deployment", 0.0);
                } else {
                    this.log("Reading Excel data...");
                    this.log("   Assigned Person: " + assignedPerson);
                    efforts = ExcelReader.parseEffortData(excelPath, assignedPerson);
                    this.log("Effort data extracted:");
                    this.log("   - Analysis: " + efforts.get("Analysis") + "h");
                    this.log("   - Design: " + efforts.get("Design") + "h");
                    this.log("   - Development: " + efforts.get("Development") + "h");
                    this.log("   - TEST: " + efforts.get("TEST") + "h");
                    this.log("   - TAS: " + efforts.get("TAS") + "h");
                    this.log("   - Documentation: " + efforts.get("Documentation") + "h");
                    this.log("   - Deployment: " + efforts.get("Deployment") + "h");
                }

                // Use already fetched CR information
                this.log("CR Info: " + this.crShortDesc);
                this.log("Looking up Assignment Group...");
                String groupSysId = this.snowClient.getAssignmentGroupSysId(assignmentGroup);
                if (groupSysId == null || groupSysId.isEmpty()) {
                    this.log("[Error] Assignment Group not found: " + assignmentGroup);
                    return;
                }

                this.log("Assignment Group found: " + assignmentGroup);
                this.log("Looking up Assigned Person...");
                String personSysId = this.snowClient.getUserSysId(assignedPerson);
                if (personSysId == null || personSysId.isEmpty()) {
                    this.log("[Error] Assigned Person not found: " + assignedPerson);
                    return;
                }

                this.log("Assigned Person found: " + assignedPerson);
                this.log("\nStarting Task creation...");

                // Use user-modified Description
                if (this.chkAnalysis.isSelected()) {
                    this.createTask(this.crSysId, "Analysis", this.crShortDesc, crDescription, groupSysId, personSysId, efforts.get("Analysis"), false);
                }

                if (this.chkDesign.isSelected()) {
                    this.createTask(this.crSysId, "Design", this.crShortDesc, crDescription, groupSysId, personSysId, efforts.get("Design"), false);
                }

                if (this.chkDevelopment.isSelected()) {
                    this.createTask(this.crSysId, "Development", this.crShortDesc, crDescription, groupSysId, personSysId, efforts.get("Development"), false);
                }

                if (this.chkTest.isSelected()) {
                    this.createTask(this.crSysId, "TEST", this.crShortDesc, crDescription, groupSysId, personSysId, efforts.get("TEST"), false);
                }

                if (this.chkTAS.isSelected()) {
                    this.createTask(this.crSysId, "TAS", this.crShortDesc, crDescription, groupSysId, personSysId, efforts.get("TAS"), false);
                }

                if (this.chkDocumentation.isSelected()) {
                    this.createTask(this.crSysId, "Documentation", this.crShortDesc, crDescription, groupSysId, personSysId, efforts.get("Documentation"), true);
                }

                if (this.chkDeployment.isSelected()) {
                    this.createTask(this.crSysId, "Deployment", this.crShortDesc, crDescription, groupSysId, personSysId, efforts.get("Deployment"), false);
                }

                this.log("\nAll Tasks created successfully!");
            } catch (Exception e) {
                this.log("[Error] " + e.getMessage());
                e.printStackTrace();
            }

        }).start();
    }

    private void createTask(String crSysId, String taskType, String crShortDesc, String crDescription, String groupSysId, String personSysId, double hours, boolean isDocument) {
        try {
            this.snowClient.createChangeTask(crSysId, taskType, crShortDesc, crDescription, groupSysId, personSysId, hours, isDocument);
            this.log("  [OK] " + taskType + " created (" + hours + "h)");
        } catch (Exception e) {
            this.log("  [Failed] " + taskType + ": " + e.getMessage());
        }
    }

    private void saveCurrentSettings() {
        AppSettings settings = SettingsManager.loadSettings();
        settings.setExcelPath(this.txtExcelPath.getText().trim());
        settings.setAssignmentGroup(this.txtAssignmentGroup.getText().trim());
        settings.setAssignedPerson(this.txtAssignedPerson.getText().trim());
        SettingsManager.saveSettings(settings);
    }

    private void log(String msg) {
        Platform.runLater(() -> this.txtLog.appendText(msg + "\n"));
    }
}
