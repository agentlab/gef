/*******************************************************************************
 * Copyright (c) 2017 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *    
 *******************************************************************************/
lexer grammar CustomHighlightingInternalDotHtmlLabelLexer;

@header {
package org.eclipse.gef.dot.internal.ui.language.highlighting.antlr.lexer;

// Hack: Use our own Lexer superclass by means of import.
// Currently there is no other way to specify the superclass for the lexer.
import org.eclipse.xtext.parser.antlr.Lexer;
}

@members {
    boolean tagMode = false;
}

RULE_HTML_COMMENT: { !tagMode }?=> ( '<!--' (~('-')|'-' ~('-')|'-' '-' ~('>'))* '-->' );
RULE_TEXT: { !tagMode }?=> ( (~('<'))+ );

RULE_TAG_START_CLOSE: { !tagMode }?=> ( '</' ) { tagMode = true; };
RULE_TAG_START      : { !tagMode }?=> ( '<'  ) { tagMode = true; };
RULE_TAG_END        : {  tagMode }?=> ( '>'  ) { tagMode = false; };
RULE_TAG_END_CLOSE  : {  tagMode }?=> ( '/>' ) { tagMode = false; };

RULE_ASSIGN    : { tagMode }?=> ( '=' );
RULE_ATTR_VALUE: { tagMode }?=> ( '"' ( options {greedy=false;} : . )* '"' );
RULE_ID        : { tagMode }?=> ( ('_'|'a'..'z'|'A'..'Z') ('_'|'a'..'z'|'A'..'Z'|'0'..'9')* );
RULE_WS        : { tagMode }?=> ( (' '|'\t'|'\n'|'\r'|'\f')+ );
