package no.novari.value.converting;

import no.novari.value.converting.model.ValueConverting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface ValueConvertingRepository extends JpaRepository<ValueConverting, Long> {

    Page<ValueConverting> findAllByFromApplicationIdIn(Pageable pageable, Set<Long> fromApplicationIds);

}
