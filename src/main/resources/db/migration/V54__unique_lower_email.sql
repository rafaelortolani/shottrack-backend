-- S15: e-mail é único sem diferenciar maiúsculas/minúsculas (Foo@x.com e foo@x.com são a mesma conta).
-- ABORTA se já houver e-mails duplicados por caixa/espaços: resolva-os antes (docs/seguranca/checar-emails-duplicados.sql).
DO $$
DECLARE
    duplicados text;
BEGIN
    SELECT string_agg(email_normalizado || ' (' || contas || ' contas)', '; ')
      INTO duplicados
      FROM (SELECT lower(btrim(email)) AS email_normalizado, count(*) AS contas
              FROM users
             GROUP BY lower(btrim(email))
            HAVING count(*) > 1) d;

    IF duplicados IS NOT NULL THEN
        RAISE EXCEPTION 'V54 abortada: e-mails duplicados por caixa/espacos em users. Una ou remova as contas antes de migrar: %', duplicados;
    END IF;
END $$;

-- a aplicação passa a gravar e comparar sempre em minúsculas e sem espaços nas pontas
UPDATE users SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));
UPDATE pending_registrations SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));
UPDATE email_verification_codes SET new_email = lower(btrim(new_email)) WHERE new_email <> lower(btrim(new_email));

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
