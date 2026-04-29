========================================
  ServiceNow CTASK Creator v1.0.0
========================================

[Requirements]
- Java 11 or higher
- Windows OS

[How to Run]
1. Double-click "run.bat"
   OR
2. Open command prompt and run:
   java -jar ctask-creator-1.0.0-shaded.jar

[Features]
- Automatically create Change Tasks (CTASK) in ServiceNow
- Read effort data from Excel files (Effort Estimation sheet)
- Supports 7 task types: Analysis, Design, Development, TEST, TAS, Documentation, Deployment

[Notes]
- Settings are saved in "app-settings.properties" file
- Excel file is optional (effort will be 0 if not selected)

[Troubleshooting]
- If the app doesn't start, make sure Java 11+ is installed
- Check Java version: java -version

========================================
