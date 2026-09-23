package com.fatec.gisa.services.usuario;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.repositories.usuario.PerfilRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PerfilService {
    
    private final PerfilRepository perfilRepository;

    @Transactional
    public List<Perfil> listarTodos() {
        return perfilRepository.findAll();
    }
}
