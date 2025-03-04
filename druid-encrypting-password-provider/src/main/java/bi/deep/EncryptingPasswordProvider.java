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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.inject.Named;
import org.apache.druid.metadata.PasswordProvider;

@Named(EncryptingPasswordProvider.TYPE_KEY)
public class EncryptingPasswordProvider implements PasswordProvider {

    public static final String TYPE_KEY = "encrypting";

    private final String encrypted;

    @JsonCreator
    public EncryptingPasswordProvider(@JsonProperty("encrypted") final String encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        try {
            return EncryptionUtils.decryptBase64(encrypted, EncryptionUtils.getSecretKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JsonProperty("encrypted")
    public String getEncrypted() {
        return encrypted;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EncryptingPasswordProvider)) {
            return false;
        }
        EncryptingPasswordProvider other = (EncryptingPasswordProvider) obj;
        return encrypted.equals(other.encrypted);
    }

    @Override
    public int hashCode() {
        return encrypted.hashCode();
    }
}
