**TabStats**


**A 1.8.9 Hypixel forge mod made for stat checking directly into your tablist!**


When you first join Hypixel with this mod, type **/api new**, then restart your game.


Join Our **Discord** for support: https://discord.gg/maxstats *(Backup https://maxstats.club/discord)*


**Images:**

![bedwars](https://user-images.githubusercontent.com/87586485/141643040-b229ad0a-f7b6-475f-a289-5ab3ce73ad10.png)
![duels](https://user-images.githubusercontent.com/87586485/141643046-9871b3d5-c9e5-4304-8230-7190c967e8e9.png)

**Currently Supported Gamemodes:**
- Bedwars
- Duels

**Plans:**
- Add an antisniper
- Add autododge
- Add Skywars, Blitz, and UHC support

**Contributors:**
- @exejar
- @yabqy
- @DirectivesMods


## Gradle wrapper troubleshooting

If you see "Could not find or load main class org.gradle.wrapper.GradleWrapperMain" when running `./gradlew`, the wrapper JAR is missing from `gradle/wrapper/gradle-wrapper.jar`.

Fixes:

- Restore the `gradle-wrapper.jar` file into `gradle/wrapper/` (it is safe to copy the jar from the Gradle repository for the project's Gradle version). For example:

```sh
cd /path/to/project
curl -fL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v6.9.1/gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
```

- Alternatively, if you have Gradle installed locally, regenerate the wrapper from the project root:

```sh
gradle wrapper
```

After restoring/regenerating the jar, run `./gradlew --version` to verify the wrapper works.
