# Eat-A-Truck 2 Simulation Instructions

This document provides instructions on how to compile, run, and use the Eat-A-Truck 2 command-line simulation.

## 1. Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Java Development Kit (JDK)**: Version 17 or higher.
- **Apache Maven**: To build the project and manage dependencies.
- **Redis Server**: The application requires a running Redis instance. You can install it locally or use a cloud-based service.

## 2. Configuration

The application is configured to connect to a Redis server at `localhost:6379`. If your Redis server is running on a different host or port, you can change these values in the `App.java` file:

```java
// src/main/java/com/eatatruck/App.java
private static final String REDIS_HOST = "localhost";
private static final int REDIS_PORT = 6379;
```

The owner's phone number is also hardcoded for the simulation in the same file.

## 3. Compilation

To compile the application, navigate to the root directory of the project (where the `pom.xml` file is located) and run the following Maven command:

```bash
mvn clean package
```

This command will compile the code, run any tests, and package the application into a single, executable JAR file with all its dependencies. The resulting file will be located at `target/eatatruck2-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## 4. Running the Application

Once the application is compiled, you can run it from the root directory using the following command:

```bash
java -jar target/eatatruck2-1.0-SNAPSHOT-jar-with-dependencies.jar
```

If the connection to Redis is successful, you will see a welcome message and a list of available commands.

## 5. Using the Simulation

The application provides an interactive command-line interface. Here are the available commands:

- `arrive`: Simulates the food truck arriving at a preset location. This is the main trigger to start the check-in flow.
- `sms <message>`: Simulates an incoming SMS from the owner. Use this to reply to the application's prompts. Example: `sms YES`.
- `addmenu <id> <price> <name> <description>`: Adds a new item to the menu in Redis.
- `showmenu`: Displays the current menu items stored in Redis.
- `help`: Shows the list of available commands.
- `exit`: Quits the simulation and closes the Redis connection.

### Example Walkthrough (Auto Campaign)

Here is a step-by-step example of a common user flow:

1.  **Start the application**:
    ```bash
    java -jar target/eatatruck2-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

2.  **Trigger the arrival**:
    ```
    > arrive
    ```
    The application will send an SMS asking if you want to open.

3.  **Reply "YES" to open**:
    ```
    > sms YES
    ```
    The application will ask if you want to customize the opening.

4.  **Reply "NO" for an auto-campaign**:
    ```
    > sms NO
    ```
    The application will post a generic campaign to social media (simulated in the console) and send a confirmation SMS. The conversation flow for this check-in is now complete.

### Example Walkthrough (Custom Campaign)

1.  Follow steps 1-3 from the auto campaign example.

2.  **Reply "YES" to customize**:
    ```
    > sms YES
    ```
    The application will show you the menu and ask you to select items.

3.  **Select menu items**:
    ```
    > sms 1, 3
    ```
    The application will create a custom campaign based on your selected items, post it, and send a confirmation. The conversation is now complete.
