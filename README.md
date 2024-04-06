# README for Five in a Row Game Project

This README provides instructions on how to set up and run both the front-end and back-end parts of the Five in a Row game project.

## Running the Front-end Project

To run the front-end project, you need to have `pnpm` package manager installed. If you haven't installed `pnpm` yet, you can install it by running `npm install -g pnpm`. After installing `pnpm`, follow the steps below:

```bash
cd path/to/five-back  # Replace path/to/frontend/project with your actual front-end project directory
pnpm i                        # Install dependencies
pnpm dev                      # Start the development server
```

## Running Database Scripts

The `init.sql` file, which initializes the database for the project, is located in the `/five-back/src/main/resources/sql` directory. To run this SQL script, you can use the command line interface of your MySQL installation. Make sure you replace `username` with your actual MySQL username:

```bash
mysql -u username -p < /path/to/init.sql  # Use the relative or absolute path to the init.sql file
```

You will be prompted to enter the password for the specified `username`. After the password is entered, the script will execute and set up the database.

## Running the Back-end Project

There are two main ways to run the back-end project: using IntelliJ IDEA or the command line.

### Using IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select `Open` and navigate to the back-end project directory.
3. Choose the `five-back` directory and open it as a project.
4. Once the project is loaded, find the main application file and run it by clicking on the green play button.

### Using Command Line

If you prefer using the command line, follow these steps:

```bash
cd path/to/backend/project  # Replace path/to/backend/project with your actual back-end project directory
mvn clean package           # Build the project and package it into a JAR file
cd target                   # Navigate to the directory containing the built JAR
java -jar jarFileName.jar   # Replace jarFileName.jar with the actual name of the JAR file
```