# Config2Brigadier
A small, JIJ-able library to automagically generate in-game command for config editing.
Useful for serverside mods.

https://user-images.githubusercontent.com/34912839/136461980-de7735d0-93c1-4fee-a805-eb8256dc476e.mp4

(generated from [below example](#usage))

## Including in your project

## Dependency
Add `jitpack.io` and nucleoid maven repository.
```gradle
repositories {
    maven { url 'https://jitpack.io' }
    maven { url 'https://maven.nucleoid.xyz' }  // For server translations API
}
```

Add `Config2Brigadier` as a dependency. Replace the `[TAG]` with the one found [here](https://github.com/samolego/Config2Brigadier/releases/latest).
```gradle
dependencies {
    // Config2Brigadier
    modImplementation include("com.github.samolego:Config2Brigadier:[TAG]")
    
    // You might need this too to translate the command messages
    modImplementation(include("xyz.nucleoid:server-translations-api:${project.server_translations_version}"))
}
```

## Usage

*See [testmod](https://github.com/samolego/Config2Brigadier/blob/master/src/test/java/org/samo_lego/config2brigader/test/fabric/)*

You need a special class that wil hold your config data. Make it implement the [`IBrigadierConfigurator`](https://github.com/samolego/Config2Brigadier/blob/master/src/main/java/org/samo_lego/config2brigadier/common/IBrigadierConfigurator.java)
interface.

Sample config class
```java
public class MyModConfig implements IBrigadierConfigurator {
    public float activationRange = 8.0F;

    public boolean show = true;

    public String message = "This is a config guide.";

    public NestedValues nested = new NestedValues();

    public static class NestedValues {
        public String message = "This is a another message.";
    }

    public List<String> randomQuestions = new ArrayList<>(Arrays.asList(
            "Why no forge port?",
            "When quilt?",
            "Tiny potato or tiny pumpkin?",
            "What is minecraft?" // How dare you
    ));
    
    // Methods that need to be overriden
    /**
     * Method called after a value is edited. The config should be saved to prevent
     * in-memory-only changes.
     */
    @Override
    public void save() {
        // Save it however you want
    }
}
```

Register the command
```java
public static class MyModCommandRegistration {
    public static final MyModConfig config = new MyModConfig();  // Or import it from elsewhere in your mod
    private static final String MOD_ID = "my_mod";

    // From event handler
    public static void registerConfigEditCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        config.generateReloadableConfigCommand(MOD_ID, dispatcher, MyModCommandRegistration::readConfig);
    }

    private static SimpleConfig readConfig() {
        return IBrigadierConfigurator.loadConfigFile(new File("config/config2brigadier_test.json"), MyModConfig.class, MyModConfig::new);
    }
}
```

This will generate the following command:
```
/my_mod
    config
        reload // <- Reloads the config
        edit
            activationRange <float>
            show <boolean>
            message <string>
            nested
                message <string>
            randomQuestions <string list>
```

## Permissions
Config2Brigadier fully supports fabric permission API. All commands require permission made of command parts, e.g.:
```
/my_mod config reload
```
will require permission `my_mod.config.reload`.

Similarly,
```
/my_mod config edit show true
```
will require permission `my_mod.config.edit.show`.

*Note*: permissions are granted by default for all OP players.

## Adding descriptions to options

If you have followed [this guide](https://quiltservertools.github.io/ServerSideDevDocs/config/gson_config/) on configs,
descriptions for values will be automatically generated from `_comment_` fields.

But there's an alternative to that. Use [`BrigadierDescription`](https://github.com/samolego/Config2Brigadier/blob/master/src/main/java/org/samo_lego/config2brigadier/common/annotation/BrigadierDescription.java)
annotation to add description and / or default field value.
```java
// Upgraded above example
public class MyModConfig implements IBrigadierConfigurator {

    @BrigadierDescription("Whether to use feature xyz.")
    public boolean show = true;

    @BrigadierDescription(value = "Message to print out on xyz event.", defaultOption = "This is a config guide.")
    public String message = "This is a config guide.";

    @Override
    public void save() {
    }
}
```

## Excluding options

Do you have any fields you want to exclude from command?

*Note; the following fields are excluded automatically:*

* static fields
* fields starting with `_comment_`

*(to change this behaviour, override [`IBrigadierConfigurator#shouldExclude(Field)`](https://github.com/samolego/Config2Brigadier/blob/038332c900fd4bf4b350261f385a5a97300900f9/src/main/java/org/samo_lego/config2brigadier/common/IBrigadierConfigurator.java#L161))*

Use [`BrigadierExcluded`](https://github.com/samolego/Config2Brigadier/blob/master/src/main/java/org/samo_lego/config2brigadier/common/annotation/BrigadierExcluded.java)
annotation.
```java
// Upgraded above example
public class MyModConfig implements IBrigadierConfigurator {
    
    // This field won't be included in command
    @BrigadierExcluded
    public boolean secretToggle = false;

    @Override
    public void save() {
    }
}
```
