package at.jku.dke.task_app.knn.data.repositories;

import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * The interface Knn task entity repository.
 */
public interface KnnTaskEntityRepository extends JpaRepository<KnnTaskEntity, Long> {
    /**
     * Find by task id optional.
     *
     * @param taskId The task id.
     * @return The optional.
     */
    Optional<KnnTaskEntity> findByTaskId(Long taskId);
}
