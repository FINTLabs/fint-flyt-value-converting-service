package no.fintlabs;

import no.fintlabs.model.ValueConverting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValueConvertingRepository extends JpaRepository<ValueConverting, Long> {
}
