package upc.edu.muusmart.healthservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Entity representing a health record for an animal.
 *
 * <p>Each record stores basic information about diseases, treatments or vaccines given to a
 * specific animal along with a penalty factor that influences production calculations. The ownerUsername field
 * records who created the entry and is used to enforce access control.</p>
 */
@Entity
@Table(name = "health_records")
@Data
public class HealthRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The identifier of the animal to which this health record belongs. */
    private Long animalId;

    /** Free text diagnosis or description of the health condition. */
    private String diagnosis;

    /** Treatment prescribed or performed (e.g., medication, procedure). */
    private String treatment;

    /** Vaccine administered, if applicable. */
    private String vaccine;

    /** Date of the health record. */
    private LocalDate date;

    /** Penalty factor used by the production service to reduce output. */
    private Double penalty;

    /** Additional notes or remarks about the condition. */
    private String notes;

    /** Username of the owner who created this record. Used to enforce access control. */
    private String ownerUsername;
}