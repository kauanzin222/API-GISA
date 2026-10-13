package com.fatec.gisa.enums;

import org.hibernate.Hibernate;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.entities.profissional.Profissional;

// Tipo de quem tem acesso ao sistema (e dos itens da listagem de profissionais)
public enum TipoCadastro {
    PROFISSIONAL,
    ESPECIALISTA,
    ESPECIALISTA_PJ,
    // Pessoa sem cadastro de profissional: hoje, só o administrador inicial criado pela migration V6
    PESSOA;

    public static TipoCadastro de(Pessoa pessoa) {
        // A herança é JOINED: o objeto pode vir como proxy da classe base
        Object real = Hibernate.unproxy(pessoa);
        if (real instanceof EspecialistaPJ) {
            return ESPECIALISTA_PJ;
        }
        if (real instanceof Especialista) {
            return ESPECIALISTA;
        }
        if (real instanceof Profissional) {
            return PROFISSIONAL;
        }
        return PESSOA;
    }
}
