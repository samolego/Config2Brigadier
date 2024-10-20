package org.samo_lego.config2brigader.test.fabric;


import com.google.gson.annotations.SerializedName;
import org.samo_lego.config2brigadier.common.IBrigadierConfigurator;
import org.samo_lego.config2brigadier.common.annotation.BrigadierDescription;
import org.samo_lego.config2brigadier.common.annotation.BrigadierExcluded;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleConfig implements IBrigadierConfigurator {

    // Naming comments in style `_comment_` + `field name`
    @SerializedName("// When to activate feature xyz.")
    public final String _comment_activationRange = "(default: 8.0)";
    @SerializedName("activation_range")
    public float activationRange = 8.0F;


    @SerializedName("// Whether to show config message.")
    public final String _comment_show0 = "";
    @SerializedName("// Another description line.")
    public final String _comment_show1 = "(default: true)";
    @SerializedName("show_config_message.")
    public boolean show = true;


    @SerializedName("config_message")
    @BrigadierDescription(value = "Which message to print out if above option is enabled.", defaultOption = "This is a config guide.")
    public String message = "This is a config guide.";

    @SerializedName("// A secret toggle that is not included in edit command.")
    public final String _comment_secretHiddenToggle0 = "(default: false)";
    @BrigadierExcluded
    public boolean secretHiddenToggle = false;

    @BrigadierDescription("Nested values")
    public NestedValues nested = new NestedValues();

    public static class NestedValues {
        public String messageNested = "This is a another message.";
    }

    @Override
    public void save() {
        System.out.println(this);
        // In your actual implementation, you would save the config to file.
    }

    public List<String> randomQuestions = new ArrayList<>(Arrays.asList(
            "Why no forge port?",
            "When quilt?",
            "Tiny potato or tiny pumpkin?",
            "What is minecraft?" // How dare you
    ));

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Field f : this.getClass().getFields()) {
            try {
                result.append(f.getName()).append(": ").append(f.get(this)).append("\n");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }
}
