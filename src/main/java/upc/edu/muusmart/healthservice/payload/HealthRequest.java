package upc.edu.muusmart.healthservice.payload;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

/**
 * Data transfer object used for creating or updating health records via the API.
 */
@Data
public class HealthRequest {
    /** Identifier of the animal associated with this health record. */
    @NotNull
    private Long animalId;

    /** Description of the diagnosis or condition. */
    private String diagnosis;

    /** Treatment applied or prescribed. */
    private String treatment;

    /** Vaccine applied, if any. */
    private String vaccine;

    /** Additional comments or notes. */
    private String notes;

    /** Date of record creation. */
    private LocalDate date;

    /** Penalty factor used by production calculations. Must be zero or positive. */
    @PositiveOrZero
    private Double penalty;
}