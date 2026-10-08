package com.fatec.gisa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SexoValidator implements ConstraintValidator<SexoValido, Character> {

    @Override
    public boolean isValid(Character sexo, ConstraintValidatorContext context) {
        return sexo == null || sexo == 'F' || sexo == 'M' || sexo == 'O';
    }
}
