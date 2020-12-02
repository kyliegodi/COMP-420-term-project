--  A user can calculate type effectiveness by inputting the attacking type, and inputting the one or two defending types.
USE pokemondb;

DROP PROCEDURE IF EXISTS type_effectiveness;

DELIMITER //
CREATE PROCEDURE type_effectiveness(IN ATTACK_TYPE INT(11), DEFEND_TYPE_1 INT(11), DEFEND_TYPE_2 INT(11))
BEGIN
    DECLARE EFFECT_VALUE1, EFFECT_VALUE2, AVG_EFFECT FLOAT DEFAULT 0;
    SET EFFECT_VALUE1 = (SELECT EFFECTIVENESS_MULTIPLIER FROM TYPE_EFFECTIVENESS
		                 WHERE DEFEND_TYPE_ID = DEFEND_TYPE_1 AND ATTACK_TYPE_ID = ATTACK_TYPE);
	SET EFFECT_VALUE2 = (SELECT EFFECTIVENESS_MULTIPLIER FROM TYPE_EFFECTIVENESS
						 WHERE DEFEND_TYPE_ID = DEFEND_TYPE_2 AND ATTACK_TYPE_ID = ATTACK_TYPE);
    SET AVG_EFFECT = IFNULL(EFFECT_VALUE1,1) * IFNULL(EFFECT_VALUE2,1);

	SELECT
    AVG_EFFECT AS "EFFECT";
END //
DELIMITER ;

CALL type_effectiveness(10,11,12);
CALL type_effectiveness(11, 10, NULL);
CALL type_effectiveness(11, 10, 6);
