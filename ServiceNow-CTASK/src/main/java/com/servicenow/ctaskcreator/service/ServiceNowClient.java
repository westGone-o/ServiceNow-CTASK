//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.servicenow.ctaskcreator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ServiceNowClient {
    private static final String BASE_URL = "https://one.service-now.com/api/now/table";
    private final HttpClient client;
    private final String authHeader;

    public ServiceNowClient(String username, String password) {
        Base64.Encoder var10001 = Base64.getEncoder();
        String var10002 = username + ":" + password;
        this.authHeader = "Basic " + var10001.encodeToString(var10002.getBytes());
        this.client = HttpClient.newBuilder().proxy(ProxySelector.getDefault()).build();
    }
    
    // Validate authentication
    public void validateConnection() throws Exception {
        String url = "https://one.service-now.com/api/now/table/sys_user?sysparm_limit=1";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", this.authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = this.client.send(request, BodyHandlers.ofString());
        
        if (response.statusCode() == 401) {
            throw new Exception("Authentication failed: Invalid ID or password.");
        } else if (response.statusCode() != 200) {
            throw new Exception("Connection failed: HTTP " + response.statusCode());
        }
    }

    public String getCRSysId(String crNumber) throws Exception {
        String url = "https://one.service-now.com/api/now/table/change_request?sysparm_query=number=" + crNumber + "&sysparm_fields=sys_id";
        String response = this.doGet(url);
        return this.parseFirstResult(response, "sys_id");
    }

    public Map<String, String> getCRDetails(String crNumber) throws Exception {
        String url = "https://one.service-now.com/api/now/table/change_request?sysparm_query=number=" + crNumber + "&sysparm_fields=sys_id,short_description,description";
        String response = this.doGet(url);
        JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
        JsonArray results = obj.getAsJsonArray("result");
        Map<String, String> details = new HashMap();
        if (results != null && results.size() > 0) {
            JsonObject cr = results.get(0).getAsJsonObject();
            details.put("sys_id", cr.get("sys_id").getAsString());
            details.put("short_description", cr.get("short_description").getAsString());
            details.put("description", cr.has("description") && !cr.get("description").isJsonNull() ? cr.get("description").getAsString() : "");
        }

        return details;
    }

    public String getUserSysId(String userName) throws Exception {
        String url = "https://one.service-now.com/api/now/table/sys_user?sysparm_query=nameLIKE" + userName.replace(" ", "%20") + "&sysparm_fields=sys_id&sysparm_limit=1";
        String response = this.doGet(url);
        return this.parseFirstResult(response, "sys_id");
    }

    public String getAssignmentGroupSysId(String groupName) throws Exception {
        String url = "https://one.service-now.com/api/now/table/sys_user_group?sysparm_query=nameLIKE" + groupName.replace(" ", "%20") + "&sysparm_fields=sys_id&sysparm_limit=1";
        String response = this.doGet(url);
        return this.parseFirstResult(response, "sys_id");
    }

    public String createChangeTask(String crSysId, String taskType, String crShortDesc, String crDescription, String assignmentGroupSysId, String assignedToSysId, double estimatedHours, boolean isDocumentation) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("change_request", crSysId);
        json.addProperty("short_description", "[" + taskType + "]" + crShortDesc);
        json.addProperty("description", crDescription);
        json.addProperty("change_task_type", "planning");
        if (assignmentGroupSysId != null && !assignmentGroupSysId.isEmpty()) {
            json.addProperty("assignment_group", assignmentGroupSysId);
        }

        if (assignedToSysId != null && !assignedToSysId.isEmpty()) {
            json.addProperty("assigned_to", assignedToSysId);
        }

        // Documentation uses Standard Effort Estimate, others use Initial Effort Estimate
        if (isDocumentation) {
            // Documentation CTASK: Standard Effort Estimate (u_revised_estimate)
            json.addProperty("u_revised_estimate", (int)estimatedHours);
        } else {
            // Regular CTASK: Initial Effort Estimate (u_initial_estimate)
            json.addProperty("u_initial_estimate", (int)estimatedHours);
        }
        
        return this.doPost("https://one.service-now.com/api/now/table/change_task", json.toString());
    }

    private String doGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", this.authHeader).header("Accept", "application/json").GET().build();
        HttpResponse<String> response = this.client.send(request, BodyHandlers.ofString());
        return (String)response.body();
    }

    private String doPost(String url, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", this.authHeader).header("Content-Type", "application/json").header("Accept", "application/json").POST(BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = this.client.send(request, BodyHandlers.ofString());
        return (String)response.body();
    }

    private String parseFirstResult(String json, String field) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        JsonArray results = obj.getAsJsonArray("result");
        return results != null && results.size() > 0 ? results.get(0).getAsJsonObject().get(field).getAsString() : null;
    }
}
