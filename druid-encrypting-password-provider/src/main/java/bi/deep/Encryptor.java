/*
 * Copyright Deep BI, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bi.deep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Encryptor {

    private static Properties loadProperties(String filename) {
        Properties properties = new Properties();
        try {
            properties.load(Files.newInputStream(Paths.get(filename)));
        } catch (IOException e) {
            System.out.println("Error loading properties file: " + filename);
            System.exit(1);
        }
        return properties;
    }

    private static void printProperties(Properties properties) {
        properties.forEach((k, v) -> System.out.println(k + "=" + v));
    }

    private static String encryptProperty(String property) throws Exception {
        // We don't want to rely on Jackson to serialize because it must be standalone JAR and I don't want to conflict
        // with Druid's Jackson version used by extensions
        return "{\"type\": \"encrypting\", \"encrypted\": \""
                + EncryptionUtils.encryptBase64(property, EncryptionUtils.getSecretKey()) + "\"}";
    }

    private static void encryptProperties(String filename, List<String> properties) {
        Properties props = loadProperties(filename);
        properties.forEach(prop -> {
            try {
                props.setProperty(prop, encryptProperty(props.getProperty(prop)));
            } catch (Exception e) {
                System.out.println("Error encrypting property: " + prop);
                System.exit(1);
            }
        });

        String loadList = props.getProperty("druid.extensions.loadList");
        if (loadList != null) {
            List<String> extensions = Arrays.stream(loadList.split(","))
                    .map(ext -> ext.replace('[', ' ').replace(']', ' ').trim())
                    .collect(Collectors.toList());
            if (!extensions.contains("\"druid-encrypting-password-provider\"")) {
                extensions.add(0, "\"druid-encrypting-password-provider\"");
                props.setProperty("druid.extensions.loadList", "[" + String.join(",", extensions) + "]");
            }
        }

        printProperties(props);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar Encryptor.jar <file.conf> <property1> [<propert2> ...]");
            System.exit(1);
        }

        encryptProperties(args[0], Arrays.asList(args).subList(1, args.length));
    }
}
