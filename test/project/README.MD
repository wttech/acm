# AEM Project testing

This test project is used for testing ACM in the AEM as a Cloud Service environment.

## Setup

1. Run script which will create fresh AEM project generated from AEM Project archetype:

    ```bash
    sh setup.sh
    ```

2. Configure AEM Cloud Manager Repository to point to this project repository.

    ```bash
    git remote add adobe "https://john-doe-acme-com:password@git.cloudmanager.adobe.com/program-name/repository-name/"
    git push adobe main -f
    ```


3. Setup Pipeline in Adobe Cloud Manager UI and select repository created in the previous step.
4. Run the pipeline to build and deploy the project to AEM as a Cloud Service environment.