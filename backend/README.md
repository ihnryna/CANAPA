# CANAPA Java Spring backend

### How to run it locally:

## Prerequisites:
### 1. IntelliJ IDEA (IDE-specific run configuration settings)
### 2. Docker Desktop (the DB container)

## 1. Clone the git repo
`git clone git@github.com:ihnryna/CANAPA.git`

Since we're working with the backend: after cloning, in the terminal, change working dir:
`cd ./CANAPA/backend`

## 2. Set up the project SDK
Open the CANAPA/backend directory as an IntelliJ IDEA project. If the "Project SDK is not defined" warning is present, go to Settings → Project Structure and set "Project SDK" to Java 25, any vendor. Additionally, make sure "Language Level" is set to either 25 or, preferably, "SDK Default".

Once the actions above are done, Gradle errors should go away, and you should be able to build the project.

The final step in this section is to copy the .env file into the CANAPA/backend directory (project root).

## 3. Build the docker container
Boot up Docker Desktop and make sure it says "engine is running".

Go back to the terminal (working dir should be project root dir, i.e. CANAPA/backend). Run `docker compose up -d`. 

The compose command should display no errors, and the Docker Desktop GUI should display a new, running "backend/canapa-postgres" container.

## 4. Verify connectivity to the database
In IntelliJ IDEA, open the "Database" window, select "Create Data Source" → "PostgreSQL". Set the "user", "password", "port" and "database" fields to the values provided in the .env file. Download the Postgres drivers, if prompted.

The database should now be present in the IDE "Database" window, and you should be able to connect to/alter it without errors.

## 5. Set up the run configuration
Lastly, in IntelliJ IDEA, click on the current run configuration (should be "BackendApplication") and from the dropdown select "Edit Configurations..."

Select "Modify options" → "Add VM options". In the "VM options" field add `-Duser.timezone=UTC`.

Select "Modify options" → "Add environment variables". Copy the contents of your .env file into the "environment variables" field and separate each entry with a semicolon, e.g.: `{entry1}={value1};{entry2}={value2};...`

Once done, run "BackendApplication" and verify that the project configuration was correct.
