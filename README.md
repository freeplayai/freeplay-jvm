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
