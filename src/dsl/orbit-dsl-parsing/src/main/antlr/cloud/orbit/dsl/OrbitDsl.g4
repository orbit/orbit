/*
 Copyright (C) 2015 - 2018 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

grammar OrbitDsl;

fragment ALPHA: [A-Za-z] ;
fragment DIGIT: [0-9] ;
fragment SPACE: [ \n\r\t] ;

/* ===== PARSER ===== */

file: declaration+ EOF ;

declaration: enumDeclaration | actorDeclaration | dataDeclaration ;

enumDeclaration: ENUM name=ID LC_BRACE members=enumMember* RC_BRACE ;
enumMember: name=ID EQUAL index=INT SEMI_COLON ;

actorDeclaration: ACTOR name=ID (L_ANGLE keyType=typeReference R_ANGLE)? LC_BRACE methods=actorMethod* RC_BRACE ;
actorMethod: returnType=typeReference name=ID L_PAREN (args=methodParam (COMMA args=methodParam)*)? R_PAREN SEMI_COLON ;
methodParam: typeReference name=ID ;

dataDeclaration: DATA name=ID LC_BRACE fields=dataField* RC_BRACE ;
dataField: typeReference name=ID EQUAL index=INT SEMI_COLON ;

typeReference: name=ID (L_ANGLE of=typeReference (COMMA of=typeReference)* R_ANGLE)? ;

/* ====== LEXER ===== */

DATA: 'data' ;
ENUM: 'enum' ;
ACTOR: 'actor' ;

ID: ALPHA(ALPHA|DIGIT|'_')* ;
INT: DIGIT+ ;

COMMA: ',' ;
EQUAL: '=' ;
L_PAREN: '(' ;
LC_BRACE: '{' ;
L_ANGLE: '<' ;
R_PAREN: ')' ;
RC_BRACE: '}' ;
R_ANGLE: '>' ;
SEMI_COLON: ';' ;

COMMENT: '//' ~[\r\n]+ -> skip ;
IGNORE: SPACE+ -> skip ;
