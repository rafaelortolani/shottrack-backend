package com.shottrack.backend.support;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Só para testes que contam comandos SQL (ligado com `spring.jpa.properties.hibernate.session_factory.statement_inspector`
 * no próprio teste): guarda o texto de cada comando preparado pelo Hibernate, para contar o total e filtrar por tabela.
 */
public class SqlRecorder implements StatementInspector {

    private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql);
        return sql;
    }

    public static void clear() {
        STATEMENTS.clear();
    }

    public static List<String> statements() {
        return List.copyOf(STATEMENTS);
    }

    public static long countContaining(String fragment) {
        return STATEMENTS.stream().filter(sql -> sql.contains(fragment)).count();
    }
}
