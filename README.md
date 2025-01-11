# Java Shell Implementation

This project is a Java-based shell simulation that interprets and executes user commands in a terminal-like environment. It supports several built-in commands and allows for external command execution.

## Features

- **Built-in Commands**:
  - `echo`: Prints or redirects the provided input.
  - `type`: Displays whether a command is a built-in or its location.
  - `pwd`: Prints the current working directory.
  - `cd`: Changes the current working directory.
  - `cat`: Concatenates and displays file content.
  - `ls`: Lists the contents of a directory.

- **Redirection and Error Handling**:
  - Supports `>` for overwriting output to files.
  - Supports `>>` for appending output to files.
  - Handles `2>` and `2>>` for error output redirection.

- **Relative and Absolute Path Handling**:
  - Supports both relative and absolute paths for file and directory operations.
  - Expands `~` to the user's home directory.

- **External Command Execution**:
  - Commands not recognized as built-in are executed as external system commands.

## Requirements

- Java Development Kit (JDK) 8 or above.

## Usage

1. Clone the repository or copy the `Main.java` file to your project.
2. Compile the program:
   ```bash
   javac Main.java
    ```
3. Run the program:
  ```bash
  java Main
  ```
4. Use the shell to execute commands. To exit, type:
  ```bash
  exit 0
  ```
## Built-in Command Details
# `echo`
Prints the provided text. Examples:
- Basic usage:
  ```bash
  echo Hello, World!
  ```
- Redirecting output:
  ```bash
  echo "This is a test" > output.txt
  ```
# `type`
Displays whether a command is a built-in or its location. Example:
  ```bash
  type echo
  ```
# `pwd`
Prints the current working directory. Example:
  ```bash
  pwd
  ```
# `cd`
Changes the current directory. Examples:
- Navigate to a specific folder:
  ```bash
  cd /path/to/directory
  ```
- Navigate to home:
  ```bash
  cd ~
  ```
# `cat`
Displays the content of files. Examples:
- Basic usage:
  ```bash
  cat file.txt
  ```
- Redirecting output:
  ```bash
  cat file.txt > output.txt
  ```
# `ls`
Lists the contents of a directory. Example:
  ```bash
  ls /path/to/directory
  ```
## Error Handling
- Displays appropriate error messages when commands or files are invalid.
- Supports redirection of errors using 2> or 2>>.
