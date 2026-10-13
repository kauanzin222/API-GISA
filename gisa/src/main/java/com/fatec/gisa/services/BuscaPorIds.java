package com.fatec.gisa.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Busca de vários registros pelos IDs enviados em uma requisição (especialidades, permissões,
// CIDs, pacientes e especialistas de uma terapia), com a mesma regra em todos os módulos:
// IDs repetidos ou nulos são unificados e, se algum não existir, nada é retornado e o erro
// lista os ausentes. O resultado segue a ordem em que os IDs foram enviados.
public final class BuscaPorIds {

    private BuscaPorIds() {
    }

    // mensagemNaoEncontrados: início da mensagem de erro, ex.: "CID(s) não encontrado(s)"
    public static <T, I> List<T> buscarTodos(Collection<I> ids, Function<Set<I>, List<T>> buscar,
            Function<T, I> idDe, String mensagemNaoEncontrados) {
        Set<I> idsUnicos = ids == null
                ? Set.of()
                : ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (idsUnicos.isEmpty()) {
            return new ArrayList<>();
        }

        Map<I, T> encontradosPorId = new LinkedHashMap<>();
        buscar.apply(idsUnicos).forEach(registro -> encontradosPorId.put(idDe.apply(registro), registro));

        List<String> naoEncontrados = idsUnicos.stream()
                .filter(id -> !encontradosPorId.containsKey(id))
                .map(String::valueOf)
                .toList();
        if (!naoEncontrados.isEmpty()) {
            throw new IllegalArgumentException(mensagemNaoEncontrados + ": " + String.join(", ", naoEncontrados));
        }

        return idsUnicos.stream().map(encontradosPorId::get).collect(Collectors.toCollection(ArrayList::new));
    }
}
