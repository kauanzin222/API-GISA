package com.fatec.gisa.config;

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class GisaPhysicalNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        return preserveCase(name);
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
        return preserveCase(name);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        return preserveCase(name);
    }

    private Identifier preserveCase(Identifier identifier) {
        if (identifier == null) {
            return null;
        }

        String text = identifier.getText();
        if (text.isEmpty() || Character.isUpperCase(text.charAt(0))) {
            return Identifier.toIdentifier(text, identifier.isQuoted());
        }

        String normalized;
        if (text.equalsIgnoreCase("cpf")) {
            normalized = "CPF";
        } else if (text.startsWith("id") && text.length() > 2
                && Character.isUpperCase(text.charAt(2))) {
            normalized = "ID" + text.substring(2);
        } else {
            normalized = text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
        }

        return Identifier.toIdentifier(normalized, identifier.isQuoted());
    }
}
