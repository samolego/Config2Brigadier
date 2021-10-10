# Config2Brigadier
A small, JIJ-able library to automagically generate in-game command for config editing.
Useful for serverside mods.

https://user-images.githubusercontent.com/34912839/136461980-de7735d0-93c1-4fee-a805-eb8256dc476e.mp4
(generated from [below example](#usage))

## Warning

A yarn contributor? Project source uses mojmap (notably mappings for command nodes and texts).

## Including in your project

## Dependency
Add `jitpack.io` maven repository.
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Depending on the modloader, add `Config2Brigadier` as a dependency. Replace the `[LATEST_VERSION]` with the one found [here](https://github.com/samolego/Config2Brigadier/releases/latest).
```gradle
dependencies {
    // Architectury (common module)
    modImplementation 'com.github.samolego:Config2Brigadier:config2brigadier:[LATEST_VERSION]'
    
    // Fabric
    modImplementation include('com.github.samolego:Config2Brigadier:config2brigadier-fabric:[LATEST_VERSION]')
    
    // Forge
    implementation fg.deobf 'com.github.samolego:Config2Brigadier:config2brigadier-forge:[LATEST_VERSION]'
    shadow('com.github.samolego:Config2Brigadier:config2brigadier-forge:[LATEST_VERSION]')
}
```

## Usage

*See [testmod](https://github.com/samolego/Config2Brigadier/tree/master/testmod-fabric/src/main/java/org/samo_lego/config2brigader/test/fabric)*

You need a special class that wil hold your config data. Make it implement the [`IBrigadierConfigurator`](https://github.com/samolego/Config2Brigadier/blob/master/common/src/main/java/org/samo_lego/config2brigadier/IBrigadierConfigurator.java)
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

// From event handler
public static class MyMod {
    public static void registerConfigEditCommand(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        LiteralCommandNode<ServerCommandSource> root = dispatcher.register(literal("mymod"));
        LiteralCommandNode<ServerCommandSource> editConfig = literal("editConfig").build();

        // Config being any object implementing API interface
        // DO NOT reassign the config field. Make it final to be sure.
        // If you reassign it, the command will still edit the old object,
        // but you'll ne using a new one.
        final MyModConfig config = new MyModConfig();
        config.generateCommand(editConfig);

        // A built-in `reload(newConfig)` method is available to be called
        // if you need to load the config values from `newConfig`.
        MyModConfig newConfig = new MyModConfig(); // or load config from disk
        config.reload(newConfig);
        // config now has newConfig values

        // Finally, add edit config node to `/mymod` command
        root.addChild(editConfig);
    }
}
```

## Adding descriptions to options

If you have followed [this guide](https://quiltservertools.github.io/ServerSideDevDocs/config/gson_config/) on configs,
descriptions for values will be automatically generated from `_comment_` fields.

But there's an alternative to that. Use [`BrigadierDescription`](https://github.com/samolego/Config2Brigadier/blob/master/common/src/main/java/org/samo_lego/config2brigadier/annotation/BrigadierDescription.java)
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

*(to change this behaviour, override [`IBrigadierConfigurator#shouldExclude(Field)`](https://github.com/samolego/Config2Brigadier/blob/421774399ed9dc1d2b50c430cc0315a6a528c48f/common/src/main/java/org/samo_lego/config2brigadier/IBrigadierConfigurator.java#L119))*

Use [`BrigadierExcluded`](https://github.com/samolego/Config2Brigadier/blob/master/common/src/main/java/org/samo_lego/config2brigadier/annotation/BrigadierExcluded.java)
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
