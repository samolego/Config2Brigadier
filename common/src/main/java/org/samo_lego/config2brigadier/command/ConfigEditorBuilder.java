package org.samo_lego.config2brigadier.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static org.samo_lego.config2brigadier.command.CommandFeedback.defaultFieldDescription;
import static org.samo_lego.config2brigadier.command.CommandGenerator.generateEditCommand;

public class ConfigEditorBuilder {
    private final LiteralCommandNode<CommandSourceStack> rootNode;
    private final Object config;
    private final Runnable saveConfigFile;
    private String commentPrefix = "_comment";
    private List<String> excludedFields = new ArrayList<>();
    private BiFunction<Object, Field, MutableComponent> commentFeedback = null;

    /**
     * Builder for /modid config edit command.
     *
     * @param editNode command node to append nodes to, usually `/modid config edit`
     * @param config config object, containing toggles and options.
     * @param saveConfigFile a runnable that saves config file.
     */
    public ConfigEditorBuilder(LiteralCommandNode<CommandSourceStack> editNode, Object config, Runnable saveConfigFile) {
        this.rootNode = editNode;
        this.config = config;
        this.saveConfigFile = saveConfigFile;
    }

    /**
     * Sets the prefix of fields that are treated as comments and will be ignored (won't be added to command). Defaults to "_comment".
     *
     * @param commentPrefix new comment prefix.
     * @return self.
     */
    public ConfigEditorBuilder setCommentPrefix(String commentPrefix) {
        this.commentPrefix = commentPrefix;
        return this;
    }

    /**
     * Sets which fields should be excluded from config. Useful if you want to
     * manually set some nodes.
     *
     * @param excludedFields a list containing all words of field names that will be excluded from generated command.
     * @return self.
     */
    public ConfigEditorBuilder setExcludedFields(List<String> excludedFields) {
        this.excludedFields = excludedFields;
        return this;
    }

    /**
     * Adds words to excluded fields. Fields in config that have a name present in this list
     * will be excluded from generated command.
     *
     * @param excludedFields all words of field names that will be excluded from generated command.
     * @return self.
     */
    public ConfigEditorBuilder addExcludedFields(String ... excludedFields) {
        Collections.addAll(this.excludedFields, excludedFields);
        return this;
    }

    /**
     * Bifunction that accepts parent object and attribute field parameters and returns
     * generated description for them. See also {@link CommandFeedback#defaultFieldDescription(Object, Field, String)}
     *
     * @param commentFeedback bifunction that generates text from parent config object and attribute field.
     * @return self.
     */
    public ConfigEditorBuilder setCommentFeedback(BiFunction<Object, Field, MutableComponent> commentFeedback) {
        this.commentFeedback = commentFeedback;
        return this;
    }

    /**
     * Builds the command.
     */
    public void build() {
        if(commentFeedback == null) {
            commentFeedback = (parent, attribute) -> defaultFieldDescription(parent, attribute, commentPrefix);
        }

        generateEditCommand(config, rootNode, saveConfigFile, commentPrefix, excludedFields, commentFeedback);
    }
}
