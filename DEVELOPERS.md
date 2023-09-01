Install Steps
================
1. `brew install jenv`
1. `echo 'export PATH="$HOME/.jenv/bin:$PATH"' >> ~/.zshrc`
1. `echo 'eval "$(jenv init -)"' >> ~/.zshrc`
1. `mkdir -p ~/.jenv/versions`
1. `jenv add /Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home/`
1. `jenv global 11.0.11`
1. `brew tap homebrew/cask-versions`
1. `brew install gradle`

Only Once (already done)
--------------------------
1. `gradle wrapper --gradle-version 8.3 --distribution-type all`


Publishing the SDK
-------------------

### Initial Setup

#### Create a Sonatype account
1. Create an account at https://issues.sonatype.org/secure/Signup!default.jspa (if you don't already have one)

#### Set up GPG
1. `brew install gnupg`
2. `gpg --gen-key`
3. `gpg --export-secret-keys > ~/.gradle/secretring.gpg`
4. `gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID`

#### Set up gradle.properties
1. Create a `gradle.properties` file in your `~/.gradle/gradle.properties` directory
2. Add the following properties to the file:
```
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_KEY_PASSWORD
signing.secretKeyRingFile=/Users/YOUR_USERNAME/.gradle/secretring.gpg

ossrhUsername=YOUR_SONATYPE_USERNAME
ossrhPassword=YOUR_SONATYPE_PASSWORD
```

### Publish to MavenLocal
This will publish the SDK to your local maven repository. This is useful for testing the SDK locally before publishing to MavenCentral.
1. Increment the version in `build.gradle.kts`
2. `./gradlew publishAllToMavenLocal`

### Publish to MavenCentral
1. Increment the version in `build.gradle.kts`
2. `./gradlew publishAll`
3. Follow the release instructions at https://central.sonatype.org/publish/release/ to close and release the repository