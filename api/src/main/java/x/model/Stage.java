package x.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/** A stage is an ordered axial sequence of body sections with attached fin sets. */
public record Stage(
        String name,
        List<BodySection> sections,
        List<MountedFinSet> fins
) {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = NoseConeSection.class,   name = "nose"),
            @JsonSubTypes.Type(value = BodyTubeSection.class,   name = "body"),
            @JsonSubTypes.Type(value = TransitionSection.class, name = "transition")
    })
    public sealed interface BodySection permits NoseConeSection, BodyTubeSection, TransitionSection {}

    public record NoseConeSection(NoseConeSpec spec) implements BodySection {}
    public record BodyTubeSection(BodyTubeSpec spec) implements BodySection {}
    public record TransitionSection(TransitionSpec spec) implements BodySection {}

    /** Fin set positioned at axial offset `xOffset` from the start of the stage. */
    public record MountedFinSet(FinSetSpec spec, double xOffset) {}
}
