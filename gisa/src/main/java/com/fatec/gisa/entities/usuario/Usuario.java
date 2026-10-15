package com.fatec.gisa.entities.usuario;

import java.time.Instant;

import com.fatec.gisa.entities.Pessoa;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@EqualsAndHashCode
@Table(name = "Usuario")
public class Usuario {

    @Id 
    @Column(name = "IDCadastro")
    private Long idCadastro;

    @OneToOne 
    @MapsId 
    @JoinColumn(name = "IDCadastro")
    private Pessoa pessoa;
    
    @Column(name = "Senha")
    private String senha;

    // Incrementada a cada troca de senha; tokens com versão anterior são recusados
    @Column(name = "VersaoToken", nullable = false)
    private int versaoToken;

    // Tokens emitidos antes da criação são recusados: um acesso retirado e concedido de novo
    // recomeça a versão em 0, e sem esta data os tokens antigos voltariam a valer
    @Column(name = "DataCriacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    // Senha gerada pelo back ao conceder acesso (ou a pedido do administrador). O login com ela não
    // devolve o token normal: o usuário precisa cadastrar uma senha nova (POST /api/auth/redefinir-senha)
    @Column(name = "SenhaTemporaria", nullable = false)
    private boolean senhaTemporaria;

    @Column(name = "SenhaTemporariaExpiraEm")
    private Instant senhaTemporariaExpiraEm;

    @ManyToOne(optional = false)
    @JoinColumn(name = "IDPerfil", nullable = false)
    private Perfil perfil;

    @PrePersist
    void aoCriar() {
        if (dataCriacao == null) {
            dataCriacao = Instant.now();
        }
    }
}
