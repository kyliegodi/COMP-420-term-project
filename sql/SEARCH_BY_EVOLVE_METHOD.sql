-- A report will be generated of a Pokemon for which Pokemon it evolves into and how each evolution is achieved
USE pokemondb;
DROP PROCEDURE IF EXISTS evolve_method;

DELIMITER //
CREATE PROCEDURE evolve_method(IN POKEMON_NUM INT(11))
BEGIN
    SELECT PREVOLVED.POKEMON_NAME              AS "PREVOLVED FORM",
           EVOLVED.POKEMON_ID                  AS "EVOLVED_ID",
           EVOLVED.POKEMON_NAME                AS "EVOLVED FORM",
           EVOLUTION_METHOD.METHOD_DESCRIPTION AS "METHOD DESCRIPTION"
    FROM EVOLUTION_METHOD
             INNER JOIN POKEMON AS PREVOLVED
                        ON (PREVOLVED.POKEMON_ID = EVOLUTION_METHOD.POKEMON_ID)
             INNER JOIN POKEMON AS EVOLVED
                        ON (EVOLVED.POKEMON_ID = EVOLUTION_METHOD.EVOLVES_INTO_ID)
    WHERE PREVOLVED.POKEMON_ID = POKEMON_NUM;
END //
DELIMITER ;

CALL evolve_method(133);
