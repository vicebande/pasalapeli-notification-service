package com.pasalapeli.notification.repository;

import com.pasalapeli.notification.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findByRol(String rol);
}
