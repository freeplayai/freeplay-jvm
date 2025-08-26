Local Development
-------------------
To configure and manage Java versions we use the following toolchain:
- jenv to manage the active version
- brew to install the jdk
- gradle to build

The following steps will configure a new machine:
1. `brew install jenv`
2. ```
   cat >> ~/.zshrc << 'EOF'
   export PATH="$HOME/.jenv/bin:$PATH"
   eval "$(jenv init -)"
   EOF```
3. `brew install gradle`
4. `brew install openjdk@11`
5. `jenv add $(/usr/libexec/java_home -v 11)`

You probably want to close and re-open a new terminal to ensure a cleanly setup environment after these steps.

Running `./gradlew --version` should look something like the following. Ensure you see JVM in the 11.0.x range.

```
------------------------------------------------------------
Gradle 8.3
------------------------------------------------------------

Build time:   2023-08-17 07:06:47 UTC
Revision:     8afbf24b469158b714b36e84c6f4d4976c86fcd5

Kotlin:       1.9.0
Groovy:       3.0.17
Ant:          Apache Ant(TM) version 1.10.13 compiled on January 4 2023
JVM:          11.0.28 (Homebrew 11.0.28+0)
OS:           Mac OS X 15.6 aarch64
```

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