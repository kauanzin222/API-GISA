package com.fatec.gisa.services.endereco;

import java.util.Set;

import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;

// Quem mora com o dono de uma lista de endereços, em cada linha de Endereco (ver EnderecoService.sincronizar)
public interface Moradia {

    // Moradores da linha que acompanham o dono: mudam junto quando ele altera o endereço
    Set<Pessoa> acompanhantes(Endereco linha);

    // Moradores da linha ligados ao dono que ficam no endereço antigo quando ele muda.
    // Não contam como "outros cadastros" (não geram 409)
    default Set<Pessoa> ficam(Endereco linha) {
        return Set.of();
    }

    static Moradia sozinho() {
        return linha -> Set.of();
    }
}
