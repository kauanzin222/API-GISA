package com.fatec.gisa.entities.paciente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "CID")
public class Cid {
    
    @Id 
    @Column(name = "CodigoCID")
    private String codigoCID;
    
    @Column(name = "Descricao")
    private String descricao;
}
