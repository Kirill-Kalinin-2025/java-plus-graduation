package ru.practicum.compilation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.compilation.model.Compilation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query("SELECT c.id FROM Compilation c " +
            "WHERE (:pinned IS NULL OR c.pinned = :pinned)")
    Page<Long> findIdsByPinned(@Param("pinned") Boolean pinned, Pageable pageable);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    @Query("SELECT DISTINCT c FROM Compilation c " +
            "LEFT JOIN c.events e " +
            "WHERE c.id IN :ids")
    List<Compilation> findAllDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    @Query("SELECT c FROM Compilation c WHERE c.id = :compId")
    Optional<Compilation> findDetailedById(@Param("compId") Long compId);
}