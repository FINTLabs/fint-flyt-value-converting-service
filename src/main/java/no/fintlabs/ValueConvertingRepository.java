package no.fintlabs;

import no.fintlabs.model.ValueConverting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValueConvertingRepository extends JpaRepository<ValueConverting, Long> {

    Page<ValueConverting> findAllByFromApplicationIdIn(Pageable pageable, List<Long> fromApplicationIds);

}
