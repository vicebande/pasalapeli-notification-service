package com.pasalapeli.notification.repository;

import com.pasalapeli.notification.entity.ReglaDistribucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaDistribucionRepository extends JpaRepository<ReglaDistribucion, Long> {

    List<ReglaDistribucion> findAllByOrderByIdAsc();

    List<ReglaDistribucion> findByEventoAndActivoTrue(String evento);

    boolean existsByEventoAndDestinoId(String evento, Long destinoId);

    void deleteByDestinoId(Long destinoId);
}
