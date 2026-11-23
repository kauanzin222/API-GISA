package com.fatec.gisa.exceptions;

import java.util.List;

// Alteração de endereço que também é de outros cadastros, sem dizer se ela vale para eles (409).
// Só a quantidade de outros cadastros vai na resposta (LGPD)
public class EnderecoCompartilhadoException extends ConflitoException {

    private final List<EnderecoCompartilhado> enderecos;

    public EnderecoCompartilhadoException(List<EnderecoCompartilhado> enderecos) {
        super("Endereço usado também por outros cadastros. Informe se a alteração vale para eles.");
        this.enderecos = List.copyOf(enderecos);
    }

    public List<EnderecoCompartilhado> getEnderecos() {
        return enderecos;
    }

    public record EnderecoCompartilhado(Long idEndereco, int outrosCadastros) {
    }
}
