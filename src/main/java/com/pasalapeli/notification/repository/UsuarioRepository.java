package com.pasalapeli.notification.repository;

import com.pasalapeli.notification.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findByRol(String rol);

    /** Fan-out ligero: solo los ids, sin cargar entidades completas. */
    @Query("SELECT u.id FROM Usuario u")
    List<Long> findAllIds();
}
