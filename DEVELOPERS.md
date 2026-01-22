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

