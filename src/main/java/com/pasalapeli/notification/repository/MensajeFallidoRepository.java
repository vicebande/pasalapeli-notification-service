package com.pasalapeli.notification.repository;

import com.pasalapeli.notification.entity.MensajeFallido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeFallidoRepository extends JpaRepository<MensajeFallido, Long> {

    List<MensajeFallido> findAllByOrderByFechaCreacionDesc();
}
