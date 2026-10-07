package com.fatec.gisa.services.paciente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.paciente.request.CidCadastroRequestDTO;
import com.fatec.gisa.entities.paciente.Cid;
import com.fatec.gisa.repositories.paciente.CidRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CidService {

    private final CidRepository cidRepository;

    @Transactional
    public List<Cid> obterOuCriar(List<String> codigosCid, List<CidCadastroRequestDTO> novosCids) {
        Map<String, Cid> resultado = new LinkedHashMap<>();

        if (codigosCid != null && !codigosCid.isEmpty()) {
            Set<String> codigosUnicos = new LinkedHashSet<>(codigosCid);
            List<Cid> encontrados = cidRepository.findAllById(codigosUnicos);
            Map<String, Cid> encontradosPorCodigo = new LinkedHashMap<>();
            encontrados.forEach(cid -> encontradosPorCodigo.put(cid.getCodigoCID(), cid));

            Set<String> codigosNaoEncontrados = new LinkedHashSet<>(codigosUnicos);
            codigosNaoEncontrados.removeAll(encontradosPorCodigo.keySet());
            if (!codigosNaoEncontrados.isEmpty()) {
                throw new IllegalArgumentException(
                        "CID(s) não encontrado(s): " + String.join(", ", codigosNaoEncontrados));
            }

            codigosUnicos.forEach(codigo -> resultado.put(codigo, encontradosPorCodigo.get(codigo)));
        }

        if (novosCids != null) {
            for (CidCadastroRequestDTO novoDto : novosCids) {
                Cid cid = resultado.get(novoDto.getCodigoCID());
                if (cid == null) {
                    cid = cidRepository.findById(novoDto.getCodigoCID())
                            .orElseGet(() -> criar(novoDto));
                    resultado.put(cid.getCodigoCID(), cid);
                }
            }
        }

        return new ArrayList<>(resultado.values());
    }

    private Cid criar(CidCadastroRequestDTO dto) {
        Cid cid = new Cid();
        cid.setCodigoCID(dto.getCodigoCID());
        cid.setDescricao(dto.getDescricao());
        return cidRepository.save(cid);
    }
}