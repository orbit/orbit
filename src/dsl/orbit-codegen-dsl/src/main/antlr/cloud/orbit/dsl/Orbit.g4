/*
 Copyright (C) 2015 - 2018 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

grammar Orbit;

fragment ALPHA: [A-Za-z] ;
fragment DIGIT: [0-9] ;
fragment SPACE: [ \n\r\t] ;

/* ===== PARSER ===== */

file: declaration+ EOF ;

declaration: enumDeclaration | actorDeclaration | dataDeclaration ;

enumDeclaration: ENUM name=ID LC_BRACE members=enumMember* RC_BRACE ;
enumMember: name=ID EQUAL index=INT SEMI_COLON;

actorDeclaration: ACTOR name=ID LC_BRACE methods=actorMethod* RC_BRACE ;
actorMethod: returnType=ID name=ID L_PAREN (args=methodParam (COMMA args=methodParam)*)? R_PAREN SEMI_COLON;
methodParam: type=ID name=ID ;

dataDeclaration: DATA name=ID LC_BRACE fields=dataField* RC_BRACE ;
dataField: type=ID name=ID EQUAL index=INT SEMI_COLON ;

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
R_PAREN: ')' ;
RC_BRACE: '}' ;
SEMI_COLON: ';' ;

COMMENT: '//' ~[\r\n]+ -> skip ;
IGNORE: SPACE+ -> skip ;
