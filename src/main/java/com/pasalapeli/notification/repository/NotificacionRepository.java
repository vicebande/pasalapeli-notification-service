package com.pasalapeli.notification.repository;

import com.pasalapeli.notification.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Deduplicacion: SQS entrega al-menos-una-vez; UNIQUE(usuario_id,tipo,referencia) respalda este check. */
    boolean existsByUsuarioIdAndTipoAndReferencia(Long usuarioId, String tipo, String referencia);

    /** Marca todas las no leidas de un usuario en un solo UPDATE (sin cargar filas a memoria). */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuarioId = :usuarioId AND n.leida = false")
    int marcarTodasLeidasPorUsuario(@Param("usuarioId") Long usuarioId);
}
