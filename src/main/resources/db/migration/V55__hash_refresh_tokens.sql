-- S06: o refresh token passa a ser guardado só como hash SHA-256 (como o token de redefinição de senha).
-- Os tokens atuais estão em texto puro e não têm como virar hash: são apagados, e todos os atletas
-- precisam entrar de novo. Nenhum valor em texto puro fica na tabela.
DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens RENAME COLUMN token TO token_hash;
