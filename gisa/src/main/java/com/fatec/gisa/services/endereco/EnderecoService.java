package com.fatec.gisa.services.endereco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.exceptions.CampoInvalidoException;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException.EnderecoCompartilhado;
import com.fatec.gisa.repositories.EnderecoRepository;

import lombok.RequiredArgsConstructor;

// Uma linha de Endereco é compartilhada por quem mora nela (CEP + número + complemento identificam o lugar)
// e só existe enquanto tiver morador. Linhas e pessoas são comparadas sempre pelo ID: Endereco e Pessoa
// têm equals por valor
@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper;

    private enum Acao { MANTER, VINCULAR, CRIAR, ALTERAR }

    private static final class Item {
        final EnderecoDTO dto;
        Endereco linha;
        Acao acao;
        Set<Long> grupo = Set.of();
        Set<Long> outros = Set.of();
        Set<Long> ficam = Set.of();

        Item(EnderecoDTO dto) {
            this.dto = dto;
        }
    }

    /**
     * Deixa os endereços de {@code dono} iguais à {@code lista} (o request substitui a lista).
     * Item sem ID: reaproveita a linha com a mesma chave ou cria uma. Item com ID: um endereço atual do dono,
     * alterado se os valores mudaram. Endereço atual ausente da lista: o dono é desligado dele.
     * Alterar uma linha que também é de outros cadastros exige {@code alterarParaTodos}; sem ele, nada é
     * gravado e todos os conflitos vão juntos num 409. Roda na transação de quem chama.
     *
     * @return se algum endereço do dono foi criado, alterado, vinculado ou removido
     */
    public boolean sincronizar(Pessoa dono, List<EnderecoDTO> lista, Moradia moradia) {
        if (dono.getEnderecos() == null) {
            dono.setEnderecos(new ArrayList<>());
        }
        List<Item> itens = (lista == null ? List.<EnderecoDTO>of() : lista).stream()
                .map(dto -> new Item(normalizar(dto)))
                .toList();
        Map<Long, Endereco> atuais = new LinkedHashMap<>();
        enderecosDe(dono).forEach(endereco -> atuais.put(endereco.getIdEndereco(), endereco));

        validar(itens, atuais);
        planejar(dono, itens, atuais, moradia);
        return aplicar(dono, itens, atuais);
    }

    // O dono deixa todos os endereços (ex.: cadastro excluído); linhas sem morador são apagadas
    public void desvincularTodos(Pessoa pessoa) {
        for (Endereco endereco : new ArrayList<>(enderecosDe(pessoa))) {
            desligar(endereco, pessoa);
            apagarSeVazio(endereco);
        }
    }

    // A pessoa passa a ter exatamente os endereços de outra (ex.: responsável que reside com o paciente)
    public void igualarEnderecos(Pessoa pessoa, Pessoa origem) {
        Set<Long> destino = ids(origem);
        for (Endereco endereco : enderecosDe(origem)) {
            ligar(endereco, pessoa);
        }
        for (Endereco anterior : new ArrayList<>(enderecosDe(pessoa))) {
            if (!destino.contains(anterior.getIdEndereco())) {
                desligar(anterior, pessoa);
                apagarSeVazio(anterior);
            }
        }
    }

    public boolean mesmosEnderecos(Pessoa uma, Pessoa outra) {
        return ids(uma).equals(ids(outra));
    }

    public boolean temEndereco(Pessoa pessoa) {
        return !enderecosDe(pessoa).isEmpty();
    }

    // ---------- Sincronização ----------

    private static void validar(List<Item> itens, Map<Long, Endereco> atuais) {
        Set<String> chaves = new HashSet<>();
        for (Item item : itens) {
            if (!chaves.add(chave(item.dto))) {
                throw new CampoInvalidoException("enderecos", "Endereço repetido na lista.");
            }
            Long id = item.dto.getIdEndereco();
            if (id != null && !atuais.containsKey(id)) {
                throw new CampoInvalidoException("enderecos", "Endereço não pertence a este cadastro.");
            }
        }
        long idsDistintos = itens.stream().map(item -> item.dto.getIdEndereco()).filter(Objects::nonNull)
                .distinct().count();
        if (idsDistintos != itens.stream().filter(item -> item.dto.getIdEndereco() != null).count()) {
            throw new CampoInvalidoException("enderecos", "Endereço repetido na lista.");
        }
    }

    // Primeira passada: decide o que fazer com cada item e calcula os conflitos, sem gravar nada
    private void planejar(Pessoa dono, List<Item> itens, Map<Long, Endereco> atuais, Moradia moradia) {
        Set<Long> comId = itens.stream().map(item -> item.dto.getIdEndereco()).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> usadas = new HashSet<>(comId);
        List<EnderecoCompartilhado> conflitos = new ArrayList<>();

        for (Item item : itens) {
            Long id = item.dto.getIdEndereco();
            if (id != null) {
                item.linha = atuais.get(id);
            } else {
                // Linhas já apontadas por outro item deste request não são reaproveitadas
                item.linha = buscarPelaChave(item.dto, usadas).orElse(null);
                if (item.linha != null) {
                    usadas.add(item.linha.getIdEndereco());
                }
            }

            if (item.linha == null) {
                item.acao = Acao.CRIAR;
                continue;
            }
            boolean jaMora = atuais.containsKey(item.linha.getIdEndereco());
            if (mesmosValores(item.dto, item.linha)) {
                item.acao = jaMora ? Acao.MANTER : Acao.VINCULAR;
                continue;
            }

            item.acao = Acao.ALTERAR;
            Set<Long> moradores = idsDosMoradores(item.linha);
            item.grupo = new HashSet<>(idsDePessoas(moradia.acompanhantes(item.linha)));
            item.grupo.retainAll(moradores);
            item.grupo.add(dono.getIdCadastro());
            item.ficam = new HashSet<>(idsDePessoas(moradia.ficam(item.linha)));
            item.ficam.retainAll(moradores);
            item.ficam.removeAll(item.grupo);
            item.outros = new HashSet<>(moradores);
            item.outros.removeAll(item.grupo);
            item.outros.removeAll(item.ficam);

            if (!item.outros.isEmpty() && item.dto.getAlterarParaTodos() == null) {
                conflitos.add(new EnderecoCompartilhado(item.linha.getIdEndereco(), item.outros.size()));
            }
        }

        if (!conflitos.isEmpty()) {
            throw new EnderecoCompartilhadoException(conflitos);
        }
    }

    // Segunda passada: remoções primeiro (para que a busca pela chave não encontre linhas que vão sumir),
    // depois criações, vínculos e alterações
    private boolean aplicar(Pessoa dono, List<Item> itens, Map<Long, Endereco> atuais) {
        boolean mudou = false;

        Set<Long> mantidas = itens.stream()
                .filter(item -> item.linha != null)
                .map(item -> item.linha.getIdEndereco())
                .collect(Collectors.toSet());
        // A1: quem reside com o dono não sai junto aqui; isso fica com quem chama (ex.: igualarEnderecos)
        for (Endereco atual : atuais.values()) {
            if (!mantidas.contains(atual.getIdEndereco())) {
                desligar(atual, dono);
                apagarSeVazio(atual);
                mudou = true;
            }
        }

        for (Item item : itens) {
            switch (item.acao) {
                case MANTER -> { }
                case VINCULAR -> {
                    ligar(item.linha, dono);
                    mudou = true;
                }
                case CRIAR -> {
                    Endereco nova = enderecoRepository.save(enderecoMapper.criarEndereco(item.dto));
                    ligar(nova, dono);
                    mudou = true;
                }
                case ALTERAR -> {
                    alterar(dono, item);
                    mudou = true;
                }
            }
        }
        return mudou;
    }

    // Muda quem está no grupo (e os outros, com alterarParaTodos = true). Se todos os moradores mudam,
    // a linha é alterada no lugar; senão, quem muda vai para outra linha e quem fica continua na antiga
    private void alterar(Pessoa dono, Item item) {
        Endereco linha = item.linha;
        Set<Long> mudam = new HashSet<>(item.grupo);
        if (Boolean.TRUE.equals(item.dto.getAlterarParaTodos())) {
            mudam.addAll(item.outros);
        }

        if (mudam.containsAll(idsDosMoradores(linha))) {
            alterarNoLugar(dono, linha, item.dto);
            return;
        }

        // Nunca a própria linha: com a mesma chave (só rua ou bairro mudaram), a busca a devolveria
        Endereco destino = buscarPelaChave(item.dto, Set.of(linha.getIdEndereco()))
                .orElseGet(() -> enderecoRepository.save(enderecoMapper.criarEndereco(item.dto)));
        for (Pessoa morador : new ArrayList<>(linha.getMoradores())) {
            if (mudam.contains(morador.getIdCadastro())) {
                desligar(linha, morador);
                ligar(destino, morador);
            }
        }
        ligar(destino, dono);
        apagarSeVazio(linha);
    }

    // Se a chave mudou e já existe outra linha com a chave nova, todos passam para ela (evita duas linhas
    // para o mesmo lugar). Se a chave não mudou, a linha fica: ela pode ter sido separada de propósito
    private void alterarNoLugar(Pessoa dono, Endereco linha, EnderecoDTO dto) {
        boolean chaveMudou = !chave(dto).equals(chave(linha));
        Optional<Endereco> outra = chaveMudou
                ? buscarPelaChave(dto, Set.of(linha.getIdEndereco()))
                : Optional.empty();

        if (outra.isPresent()) {
            Endereco destino = outra.get();
            for (Pessoa morador : new ArrayList<>(linha.getMoradores())) {
                desligar(linha, morador);
                ligar(destino, morador);
            }
            ligar(destino, dono);
            apagarSeVazio(linha);
            return;
        }

        copiarValores(dto, linha);
        ligar(linha, dono);
    }

    // ---------- Vínculos (os dois lados ficam iguais em memória: o mapper lê pessoa.getEnderecos()) ----------

    private static void ligar(Endereco endereco, Pessoa pessoa) {
        if (endereco.getMoradores() == null) {
            endereco.setMoradores(new ArrayList<>());
        }
        if (endereco.getMoradores().stream().noneMatch(morador -> mesmaPessoa(morador, pessoa))) {
            endereco.getMoradores().add(pessoa);
        }
        if (pessoa.getEnderecos() == null) {
            pessoa.setEnderecos(new ArrayList<>());
        }
        if (pessoa.getEnderecos().stream().noneMatch(atual -> mesmaLinha(atual, endereco))) {
            pessoa.getEnderecos().add(endereco);
        }
    }

    private static void desligar(Endereco endereco, Pessoa pessoa) {
        if (endereco.getMoradores() != null) {
            endereco.getMoradores().removeIf(morador -> mesmaPessoa(morador, pessoa));
        }
        if (pessoa.getEnderecos() != null) {
            pessoa.getEnderecos().removeIf(atual -> mesmaLinha(atual, endereco));
        }
    }

    private void apagarSeVazio(Endereco endereco) {
        if (endereco.getMoradores() == null || endereco.getMoradores().isEmpty()) {
            enderecoRepository.delete(endereco);
        }
    }

    private static boolean mesmaPessoa(Pessoa uma, Pessoa outra) {
        return uma == outra || (uma.getIdCadastro() != null && uma.getIdCadastro().equals(outra.getIdCadastro()));
    }

    private static boolean mesmaLinha(Endereco uma, Endereco outra) {
        return uma == outra
                || (uma.getIdEndereco() != null && uma.getIdEndereco().equals(outra.getIdEndereco()));
    }

    // ---------- Chave e normalização ----------

    private Optional<Endereco> buscarPelaChave(EnderecoDTO dto, Set<Long> ignorar) {
        return enderecoRepository.buscarPelaChave(dto.getCep(), dto.getNumero(), dto.getComplemento()).stream()
                .filter(endereco -> !ignorar.contains(endereco.getIdEndereco()))
                .findFirst();
    }

    // trim em tudo, CEP só com dígitos e complemento vazio como null
    private static EnderecoDTO normalizar(EnderecoDTO dto) {
        String complemento = limpar(dto.getComplemento());
        return new EnderecoDTO(
                dto.getIdEndereco(),
                dto.getCep() == null ? null : dto.getCep().replaceAll("\\D", ""),
                limpar(dto.getRua()),
                limpar(dto.getNumero()),
                complemento == null || complemento.isEmpty() ? null : complemento,
                limpar(dto.getBairro()),
                limpar(dto.getCidade()),
                limpar(dto.getEstado()),
                dto.getAlterarParaTodos());
    }

    private static String limpar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String chave(EnderecoDTO dto) {
        return chave(dto.getCep(), dto.getNumero(), dto.getComplemento());
    }

    private static String chave(Endereco endereco) {
        return chave(endereco.getCep() == null ? null : endereco.getCep().replaceAll("\\D", ""),
                limpar(endereco.getNumero()), limpar(endereco.getComplemento()));
    }

    private static String chave(String cep, String numero, String complemento) {
        return String.join("|",
                Objects.toString(cep, ""),
                Objects.toString(numero, "").toLowerCase(Locale.ROOT),
                Objects.toString(complemento, "").toLowerCase(Locale.ROOT));
    }

    private static boolean mesmosValores(EnderecoDTO dto, Endereco endereco) {
        return chave(dto).equals(chave(endereco))
                && Objects.equals(dto.getRua(), limpar(endereco.getRua()))
                && Objects.equals(dto.getBairro(), limpar(endereco.getBairro()))
                && Objects.equals(dto.getCidade(), limpar(endereco.getCidade()))
                && Objects.equals(dto.getEstado(), limpar(endereco.getEstado()));
    }

    private static void copiarValores(EnderecoDTO dto, Endereco endereco) {
        endereco.setCep(dto.getCep());
        endereco.setRua(dto.getRua());
        endereco.setNumero(dto.getNumero());
        endereco.setComplemento(dto.getComplemento());
        endereco.setBairro(dto.getBairro());
        endereco.setCidade(dto.getCidade());
        endereco.setEstado(dto.getEstado());
    }

    // ---------- Apoio ----------

    private static List<Endereco> enderecosDe(Pessoa pessoa) {
        return pessoa.getEnderecos() == null ? List.of() : pessoa.getEnderecos();
    }

    private static Set<Long> ids(Pessoa pessoa) {
        return enderecosDe(pessoa).stream().map(Endereco::getIdEndereco).collect(Collectors.toSet());
    }

    private static Set<Long> idsDosMoradores(Endereco endereco) {
        return endereco.getMoradores() == null ? Set.of() : idsDePessoas(endereco.getMoradores());
    }

    private static Set<Long> idsDePessoas(Collection<Pessoa> pessoas) {
        return pessoas.stream().map(Pessoa::getIdCadastro).collect(Collectors.toSet());
    }
}
