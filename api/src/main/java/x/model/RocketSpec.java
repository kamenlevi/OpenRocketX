package x.model;

import java.util.List;

/**
 * Top-level rocket description for the export pipeline. Single-stage is the
 * common case; multi-stage is supported in the model but V1 export composes
 * stages tip-to-base along +X.
 */
public record RocketSpec(
        String name,
        List<Stage> stages
) {
}
