# AEM Project testing

This test project is used for testing ACM in the AEM as a Cloud Service environment.

1. Configure AEM Cloud Manager Repository in *.env* file in the root of the repository:
    ```env
    AEM_CM_URL=https://<username>:<password>@git.cloudmanager.adobe.com/<program-name>/<repository-name>/
    ```
    Replace `<username>`, `<password>`, `<program-name>`, and `<repository-name>` with your actual Cloud Manager credentials and repository details.


2. Run script by task from the root of the repository:

    ```bash
    sh taskw test:project
    ```

3. Setup Pipeline in Adobe Cloud Manager UI and select repository created in the previous step.
4. Run the pipeline to build and deploy the project to AEM as a Cloud Service environment.