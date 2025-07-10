Publishing the SDK
-------------------

### Initial Setup

#### Create a Central Publisher Portal account

1. Create an account at https://central.sonatype.com/

#### Set up GPG (unchanged)

1. `brew install gnupg`
2. `gpg --gen-key`
3. `gpg --export-secret-keys > ~/.gradle/secretring.gpg`
4. `chmod 600 ~/.gradle/secretring.gpg`
5. `gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID`
    * YOUR_KEY_ID should be the last 8 digits of your gpg public key from above.

#### Generate Portal Token

1. Log in to https://central.sonatype.com/publishing
2. Go to "View Account" → "Generate User Token"
3. Save the username/password pair

#### Set up gradle.properties

1. Create a `gradle.properties` file in your `~/.gradle/gradle.properties` directory
2. `chmod 600 ~/.gradle/gradle.properties`
3. Add the following properties to the file:

```properties
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_KEY_PASSWORD
signing.secretKeyRingFile=/Users/YOUR_USERNAME/.gradle/secretring.gpg
ossrhUsername=YOUR_PORTAL_TOKEN_USERNAME
ossrhPassword=YOUR_PORTAL_TOKEN_PASSWORD
```

### Publish to MavenCentral

1. Increment the version in `build.gradle.kts`
2. `./gradlew publishAll`
3. Log in to https://central.sonatype.com/publishing/deployments and click "Publish".