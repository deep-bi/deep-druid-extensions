# Druid Encrypting Password Provider

This is a password provider extension for [Apache Druid](https://druid.apache.org/) that encrypts runtime configuration passwords using AES algorithm
with a hardcoded encryption key.
The Encrypting Password Provider will automatically replace password values with `{"type": "encrypting", "encrypted": "ENCRYPTED_PASSWORD"}` entry 
(where `ENCRYPTED_PASSWORD` is the password encrypted with AES).
See more details at https://druid.apache.org/docs/latest/operations/password-provider.html

## Usage

The application serves as extension to read encrypted files and to encrypt existing files. First let's encrypt the selected file:

```bash
java -jar druid-encrypting-password-provider-25.0.0.jar INPUT_FILE_PATH PROPERTY_1 [PROPERTY_2 ...]
```

The output of the command will be printed to the standard output. You can redirect it to a file if you want to, e.g.:

```bash
java -jar druid-encrypting-password-provider-25.0.0.jar INPUT_FILE_PATH PROPERTY_1 [PROPERTY_2 ...] > OUTPUT_FILE_PATH
```

Example:

```bash
java -jar druid-encrypting-password-provider-25.0.0.jar common.runtime.properties druid.metadata.storage.connector.password druid.auth.basic.ssl.trustStorePassword
```

The application automatically detects the `druid.extensions.loadList` property and adds itself to the list as the first extension.

## Configuration

To use the extension, you need to add the extension jar to the Druid classpath and add it to the list of extensions in the Druid configuration file:

```hocon
druid.extensions.loadList=["druid-encrypting-password-provider"]
```

The `loadList` is extended automatically when you use the application to encrypt a file, so you don't need to do it manually.

To add the jar to the classpath just copy the file under `DRUID_PATH/extensions/druid-encrypting-password-provider/`.
