# Gradle Versioned Remote Script Plugin Example

This source code is part of [Gradle build tool example projects](https://github.com/rivancic/gradle).

It contains Gradle script plugin that will be used in the lecture explaining script plugins.

Why it is named versioned remote script plugin?

- **Script plugin**: First it's a script plugin that can be applied in build.gradle script. It contains files tasks that you should be 
familiar with since the tasks lecture.
- **Remote**: It's meant to be accessed over network to remotely located script plugin here in Github code repository. The 
repository is publicly available, everyone has access to it so everyone can include script in their own gradle script.
- **Versioned**: One way to version the process of evolving this plugin is to use handy git tags. With git tags we can add
changes to the remote script plugin. If script plugin is referenced with the tag name then existing usage of this plugin 
  is not affected with possible breaking changes in the future.