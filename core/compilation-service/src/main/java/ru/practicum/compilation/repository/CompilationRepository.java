package ru.practicum.compilation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.compilation.model.Compilation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query("SELECT c.id FROM Compilation c WHERE (:pinned IS NULL OR c.pinned = :pinned)")
    Page<Long> findIdsByPinned(@Param("pinned") Boolean pinned, Pageable pageable);

    @Query("SELECT c FROM Compilation c WHERE c.id IN :ids")
    List<Compilation> findAllDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT c FROM Compilation c WHERE c.id = :compId")
    Optional<Compilation> findDetailedById(@Param("compId") Long compId);
}