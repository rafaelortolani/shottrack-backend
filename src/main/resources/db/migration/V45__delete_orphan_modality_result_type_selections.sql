-- UC12/ADR-0011: remover uma modalidade praticada não apagava as seleções
-- de tipo de resultado dela, e as linhas órfãs faziam readicionar a mesma
-- modalidade violar a constraint única (500). O código passou a apagar as
-- seleções junto; aqui limpa as órfãs que já existem.
DELETE FROM modality_result_type_selections s
WHERE NOT EXISTS (
    SELECT 1
    FROM practiced_modalities p
    WHERE p.user_id = s.user_id
      AND p.modality_id = s.modality_id
);
