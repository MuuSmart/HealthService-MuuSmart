package upc.edu.muusmart.healthservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upc.edu.muusmart.healthservice.model.HealthRecord;

import java.util.List;

/**
 * Repository interface for accessing health records stored in the database.
 */
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    /**
     * Returns all health records for a given animal.
     *
     * @param animalId the animal identifier
     * @return list of health records
     */
    List<HealthRecord> findByAnimalId(Long animalId);

    /**
     * Returns all health records created by a specific user.
     *
     * @param ownerUsername the username of the record owner
     * @return list of health records
     */
    List<HealthRecord> findByOwnerUsername(String ownerUsername);
}