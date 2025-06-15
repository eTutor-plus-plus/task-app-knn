package at.jku.dke.task_app.knn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * This class represents a data transfer object for submitting a solution.
 *
 * @param input The user input.
 */
public record KnnSubmissionDto(@NotNull @Size(max = 255) String input) {
}
