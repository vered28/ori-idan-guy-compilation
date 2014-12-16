package IC.Parser;

%%

%cup
%eofval{
	return new Token(sym.EOF, yyline+1,yycolumn+1,yytext(),yytext());
%eofval}

%class Lexer
%public
%function next_token
%type Token
%line
%column
%scanerror LexicalError

%{
	public int getLineNumber() { return yyline+1; }
	public int getColumnNumber() { return yycolumn+1; }
%}

LOWER_ALPHA=[a-z]
UPPER_ALPHA=[A-Z]
POS_DIGIT=[1-9]
DIGIT={POS_DIGIT}|0
POS_NUMBER = ({POS_DIGIT}({DIGIT}*))
NUMBER = {POS_NUMBER}|0+

NEWLINE=(\r?\n)|\r
STRING=\"(.*?)\"
WHITESPACE=([ ]|[\t]|{NEWLINE})+
ALPHA=({LOWER_ALPHA}|{UPPER_ALPHA})+
ALPHA_NUMERIC={LOWER_ALPHA}({ALPHA}|{NUMBER}|_)*
CLASS_ID={UPPER_ALPHA}({ALPHA}|{NUMBER}|_)*
LINE_COMMENT=\/\/.*{NEWLINE}
BLOCK_COMMENT=\/\*([^*]|\*[^\/])*\*\/
COMMENT={LINE_COMMENT}|{BLOCK_COMMENT}

BAD_STRING_1 = \"([\\][\"]|[^\"\n\r])*{NEWLINE}
BAD_STRING_2 = \"(([^\\]|[\\][nrt\"])*[\\][^\"nrt])\"
BAD_STRING_3 = \"(.*[\\][ ].*)\"
BAD_STRING = {BAD_STRING_1} | {BAD_STRING_2} | {BAD_STRING_3}
BAD_INTEGER = 0+{POS_NUMBER}
COMMENT_WITH_NO_END = \/\*([^*]|\*[^\/])
COMMENT_WITH_NO_START = \*\/

%%

"(" { return new Token(sym.LPAREN,yyline+1,yycolumn+1,yytext(),yytext()); }
")" { return new Token(sym.RPAREN,yyline+1,yycolumn+1,yytext(),yytext()); }
"[" { return new Token(sym.LBRACK,yyline+1,yycolumn+1,yytext(),yytext()); }
"]" { return new Token(sym.RBRACK,yyline+1,yycolumn+1,yytext(),yytext()); }
"{" { return new Token(sym.LBRACE,yyline+1,yycolumn+1,yytext(),yytext()); }
"}" { return new Token(sym.RBRACE,yyline+1,yycolumn+1,yytext(),yytext()); }

{BAD_STRING} { throw new LexicalError("illegal string " + yytext().trim() + " at " + (yyline+1) + ":" + (yycolumn+1)); }
{BAD_INTEGER} { throw new LexicalError("illegal integer " + yytext() + " at " + (yyline+1) + ":" + (yycolumn+1)); }
{COMMENT_WITH_NO_END} { throw new LexicalError("comment starting at " + (yyline+1) + ":" + (yycolumn+1) + " is missing closure '*/'"); }
{COMMENT_WITH_NO_START} { throw new LexicalError("comment closure at " + (yyline+1) + ":" + (yycolumn+1) + " has no beginning (not a legal comment)."); }

{NUMBER} { return new Token(sym.INTEGER, yyline+1,yycolumn+1,"INTEGER",yytext()); }

{WHITESPACE} | {COMMENT} { }

";" { return new Token(sym.SEMICOLON,yyline+1,yycolumn+1,yytext(),yytext()); }
"," { return new Token(sym.COMMA, yyline+1,yycolumn+1,yytext(),yytext()); }

"class" { return new Token(sym.CLASS,yyline+1,yycolumn+1,yytext(),yytext()); }
"extends" { return new Token(sym.EXTENDS,yyline+1,yycolumn+1,yytext(),yytext()); }
"static" { return new Token(sym.STATIC,yyline+1,yycolumn+1,yytext(),yytext()); }
"void" { return new Token(sym.VOID,yyline+1,yycolumn+1,yytext(),yytext()); }
"int" { return new Token(sym.INT,yyline+1,yycolumn+1,yytext(),yytext()); }
"boolean" { return new Token(sym.BOOLEAN,yyline+1,yycolumn+1,yytext(),yytext()); }
"string" { return new Token(sym.STRING,yyline+1,yycolumn+1,yytext(),yytext()); }
"if" { return new Token(sym.IF,yyline+1,yycolumn+1,yytext(),yytext()); }
"else" { return new Token(sym.ELSE,yyline+1,yycolumn+1,yytext(),yytext()); }
"while" { return new Token(sym.WHILE,yyline+1,yycolumn+1,yytext(),yytext()); }
"break" { return new Token(sym.BREAK,yyline+1,yycolumn+1,yytext(),yytext()); }
"continue" { return new Token(sym.CONTINUE,yyline+1,yycolumn+1,yytext(),yytext()); }
"this" { return new Token(sym.THIS,yyline+1,yycolumn+1,yytext(),yytext()); }
"new" { return new Token(sym.NEW,yyline+1,yycolumn+1,yytext(),yytext()); }
"length" { return new Token(sym.LENGTH,yyline+1,yycolumn+1,yytext(),yytext()); }
"true" { return new Token(sym.TRUE,yyline+1,yycolumn+1,yytext(),yytext()); }
"false" { return new Token(sym.FALSE,yyline+1,yycolumn+1,yytext(),yytext()); }
"null" { return new Token(sym.NULL,yyline+1,yycolumn+1,yytext(),yytext()); }
"return" { return new Token(sym.RETURN,yyline+1,yycolumn+1,yytext(),yytext()); }

"!=" { return new Token(sym.OP_NEQ,yyline+1,yycolumn+1,yytext(),yytext()); }

"\." { return new Token(sym.OP_DOT,yyline+1,yycolumn+1,yytext(),yytext()); }
"-" { return new Token(sym.OP_MINUS,yyline+1,yycolumn+1,yytext(),yytext()); }
"!" { return new Token(sym.OP_NOT,yyline+1,yycolumn+1,yytext(),yytext()); }
"\*" { return new Token(sym.OP_MULT,yyline+1,yycolumn+1,yytext(),yytext()); }
"/" { return new Token(sym.OP_DIV,yyline+1,yycolumn+1,yytext(),yytext()); }
"%" { return new Token(sym.OP_MOD,yyline+1,yycolumn+1,yytext(),yytext()); }
"\+" { return new Token(sym.OP_ADD,yyline+1,yycolumn+1,yytext(),yytext()); }

"<=" { return new Token(sym.OP_LEQ,yyline+1,yycolumn+1,yytext(),yytext()); }
"<" { return new Token(sym.OP_LT,yyline+1,yycolumn+1,yytext(),yytext()); }
">=" { return new Token(sym.OP_GEQ,yyline+1,yycolumn+1,yytext(),yytext()); }
">" { return new Token(sym.OP_GT,yyline+1,yycolumn+1,yytext(),yytext()); }
"==" { return new Token(sym.OP_EQ,yyline+1,yycolumn+1,yytext(),yytext()); }
"&&" { return new Token(sym.OP_AND,yyline+1,yycolumn+1,yytext(),yytext()); }
"||" { return new Token(sym.OP_OR,yyline+1,yycolumn+1,yytext(),yytext()); }
"=" { return new Token(sym.OP_ASSIGNMENT,yyline+1,yycolumn+1,yytext(),yytext()); }

{CLASS_ID} { return new Token(sym.CLASS_ID,yyline+1,yycolumn+1,"CLASS_ID",yytext()); }
{ALPHA_NUMERIC} { return new Token(sym.IDENTIFIER,yyline+1,yycolumn+1,"ID",yytext()); }
{STRING} {
			String str = yytext();
			char[] escapes = { 9, /*tab*/
							   10, /* line feeder */
							   13 /* carriage return */ };
			str = str.replace("\\t", escapes[0] + "");
			str = str.replace("\\n", escapes[1] + "");
			str = str.replace("\\r", escapes[2] + "");
			return new Token(sym.STR,yyline+1,yycolumn+1,"STRING",str);
		 }
