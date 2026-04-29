//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.servicenow.ctaskcreator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleSheetClient {
    public static String extractSheetId(String url) {
        Pattern pattern = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid Google Sheet URL");
        }
    }

    public static Map<String, Double> parseEffortData(String sheetUrl, String picName, String apiKey) throws Exception {
        String sheetId = extractSheetId(sheetUrl);
        String apiUrl = String.format("https://sheets.googleapis.com/v4/spreadsheets/%s/values/Effort%%20Estimation!A1:G100?key=%s", sheetId, apiKey);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Accept", "application/json").GET().build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            int var10002 = response.statusCode();
            throw new Exception("Google Sheets API Error (" + var10002 + "): " + (String)response.body());
        } else {
            JsonObject jsonResponse = JsonParser.parseString((String)response.body()).getAsJsonObject();
            JsonArray rows = jsonResponse.getAsJsonArray("values");
            if (rows != null && rows.size() != 0) {
                Map<String, Double> efforts = new HashMap();
                efforts.put("Analysis", (double)0.0F);
                efforts.put("Design", (double)0.0F);
                efforts.put("Development", (double)0.0F);
                efforts.put("Test", (double)0.0F);
                efforts.put("Document", (double)0.0F);
                efforts.put("Deployment", (double)0.0F);
                JsonArray header = rows.get(0).getAsJsonArray();
                int typeCol = -1;
                int effortCol = -1;
                int picCol = -1;

                for(int i = 0; i < header.size(); ++i) {
                    String col = header.get(i).getAsString().trim();
                    if (col.equals("Type")) {
                        typeCol = i;
                    }

                    if (col.contains("Estimated M/H") || col.contains("Estimated")) {
                        effortCol = i;
                    }

                    if (col.equals("PIC")) {
                        picCol = i;
                    }
                }

                if (typeCol != -1 && effortCol != -1 && picCol != -1) {
                    String currentType = null;

                    for(int i = 1; i < rows.size(); ++i) {
                        JsonArray row = rows.get(i).getAsJsonArray();
                        if (row.size() > Math.max(typeCol, Math.max(effortCol, picCol))) {
                            String type = row.size() > typeCol ? row.get(typeCol).getAsString().trim() : "";
                            String effortStr = row.size() > effortCol ? row.get(effortCol).getAsString().trim() : "0";
                            String pic = row.size() > picCol ? row.get(picCol).getAsString().trim() : "";
                            if (!type.isEmpty()) {
                                currentType = type;
                            }

                            if (pic.contains(picName) && currentType != null) {
                                double effort = parseEffort(effortStr);
                                String typeKey = currentType.replaceAll("\\s+", "");
                                if (typeKey.equalsIgnoreCase("Analysis")) {
                                    efforts.put("Analysis", (Double)efforts.get("Analysis") + effort);
                                } else if (typeKey.equalsIgnoreCase("Design")) {
                                    efforts.put("Design", (Double)efforts.get("Design") + effort);
                                } else if (typeKey.equalsIgnoreCase("Development")) {
                                    efforts.put("Development", (Double)efforts.get("Development") + effort);
                                } else if (!typeKey.equalsIgnoreCase("Test") && !typeKey.equalsIgnoreCase("TEST")) {
                                    if (typeKey.equalsIgnoreCase("Document")) {
                                        efforts.put("Document", (Double)efforts.get("Document") + effort);
                                    } else if (typeKey.equalsIgnoreCase("Deployment")) {
                                        efforts.put("Deployment", (Double)efforts.get("Deployment") + effort);
                                    }
                                } else {
                                    efforts.put("Test", (Double)efforts.get("Test") + effort);
                                }
                            }
                        }
                    }

                    return efforts;
                } else {
                    throw new Exception("Required columns not found (Type, Estimated M/H, PIC)");
                }
            } else {
                throw new Exception("No data found in sheet.");
            }
        }
    }

    private static double parseEffort(String effortStr) {
        try {
            return Double.parseDouble(effortStr.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException var2) {
            return (double)0.0F;
        }
    }
}
