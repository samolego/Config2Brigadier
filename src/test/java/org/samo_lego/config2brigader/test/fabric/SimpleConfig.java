package org.samo_lego.config2brigader.test.fabric;

import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Direction;
import org.samo_lego.config2brigadier.common.IBrigadierConfigurator;
import org.samo_lego.config2brigadier.common.annotation.BrigadierDescription;
import org.samo_lego.config2brigadier.common.annotation.BrigadierExcluded;

public class SimpleConfig implements IBrigadierConfigurator {

    // Naming comments in style `_comment_` + `field name`
    @SerializedName("// When to activate feature xyz.")
    public final String _comment_activationRange = "(default: 8.0)";

    @SerializedName("activation_range")
    public float activationRange = 8.0F;

    @SerializedName(
        "// A mapping of keys to values. This is an example of a map integer field."
    )
    public final String _comment_mappingInt = "";

    @SerializedName("mapping_int")
    public Map<String, Integer> mappingInt = Map.of("key1", 1, "key2", 2);

    // Map int to nested object
    @SerializedName(
        "// A mapping of keys to nested values. This is an example of a map integer field with nested values."
    )
    public final String _comment_nestedMap = "";

    @SerializedName("nested_map")
    public Map<Integer, NestedValues> nestedMap = Map.of(
        1,
        new NestedValues(),
        2,
        new NestedValues()
    );

    // Set of floats
    @SerializedName(
        "// A set of floats. This is an example of a set float field."
    )
    public final String _comment_floatBag = "";

    @SerializedName("float_bag")
    public Set<Float> floatBag = Set.of(1.0F, 2.0F, 3.0F);

    @SerializedName("// Whether to show config message.")
    public final String _comment_show0 = "";

    @SerializedName("// Another description line.")
    public final String _comment_show1 = "(default: true)";

    @SerializedName("show_config_message.")
    public boolean show = true;

    @SerializedName("config_message")
    @BrigadierDescription(
        value = "Which message to print out if above option is enabled.",
        defaultOption = "This is a config guide."
    )
    public String message = "This is a config guide.";

    @SerializedName("// A secret toggle that is not included in edit command.")
    public final String _comment_secretHiddenToggle0 = "(default: false)";

    @BrigadierExcluded
    public boolean secretHiddenToggle = false;

    @BrigadierDescription("Nested values")
    public NestedValues nested = new NestedValues();

    public static class NestedValues {

        public String messageNested = "This is a another message.";
        public float customThreshold = 0.2f;
        public NestedSquared anotherLayer = new NestedSquared();

        public static class NestedSquared {
            public String endOfWorld = "No more fields here";
            public Direction direction = Direction.NORTH;
            @BrigadierExcluded
            public int superSecret = 42;
        }
    }

    @Override
    public void save() {
        System.out.println(this);
        // In your actual implementation, you would save the config to file.
    }

    public List<String> randomQuestions = new ArrayList<>(
        Arrays.asList(
            "Why no forge port?",
            "When quilt?",
            "Tiny potato or tiny pumpkin?",
            "What is minecraft?" // How dare you
        )
    );

    public List<NestedValues> nestedList = new ArrayList<>(
        Arrays.asList(new NestedValues(), new NestedValues())
    );

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Field f : this.getClass().getFields()) {
            try {
                result
                    .append(f.getName())
                    .append(": ")
                    .append(f.get(this))
                    .append("\n");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }
}
