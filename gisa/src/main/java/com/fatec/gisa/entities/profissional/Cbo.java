package com.fatec.gisa.entities.profissional;

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
@Table(name = "CBO")
public class Cbo {

    @Id 
    @Column(name = "CodigoCBO")
    private String codigoCBO;
    
    @Column(name = "TituloCBO")
    private String tituloCBO;
}
