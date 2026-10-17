package com.pasalapeli.notification.repository;

import com.pasalapeli.notification.entity.DestinoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DestinoNotificacionRepository extends JpaRepository<DestinoNotificacion, Long> {

    List<DestinoNotificacion> findAllByOrderByIdAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
