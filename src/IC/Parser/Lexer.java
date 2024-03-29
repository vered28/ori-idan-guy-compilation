/* The following code was generated by JFlex 1.6.0 */

package IC.Parser;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.6.0
 * from the specification file <tt>J:/Documents and Settings/Owner/workspace/Compilation02/src/IC/Parser/IC.lex</tt>
 */
public class Lexer implements java_cup.runtime.Scanner {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0, 0
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\11\0\1\12\1\6\1\15\1\15\1\5\22\0\1\11\1\53\1\7"+
    "\2\0\1\57\1\63\1\0\1\20\1\21\1\16\1\60\1\27\1\56"+
    "\1\55\1\14\1\4\11\3\1\0\1\26\1\61\1\54\1\62\2\0"+
    "\32\2\1\22\1\10\1\23\1\0\1\13\1\0\1\32\1\44\1\30"+
    "\1\40\1\34\1\46\1\45\1\50\1\41\1\1\1\51\1\31\1\1"+
    "\1\37\1\43\2\1\1\17\1\33\1\36\1\52\1\42\1\47\1\35"+
    "\2\1\1\24\1\64\1\25\7\0\1\15\u1fa2\0\1\15\1\15\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\udfe6\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\1\1\1\2\2\3\1\4\1\0\1\5\1\6"+
    "\1\1\1\7\1\10\1\11\1\12\1\13\1\14\1\15"+
    "\1\16\13\1\1\17\1\20\1\21\1\22\1\23\1\24"+
    "\1\25\1\26\2\0\1\27\2\30\1\31\4\0\1\32"+
    "\14\1\1\33\5\1\1\34\1\35\1\36\1\37\1\40"+
    "\1\41\5\0\2\30\1\31\3\0\1\31\1\0\2\4"+
    "\1\42\1\0\12\1\1\43\1\1\1\44\5\1\5\0"+
    "\3\30\2\0\2\30\2\0\1\31\3\0\6\1\1\45"+
    "\1\1\1\46\1\47\1\50\1\51\4\1\2\0\3\31"+
    "\1\30\1\31\1\30\1\1\1\52\5\1\1\53\1\1"+
    "\1\54\1\55\1\56\1\1\1\57\1\60\1\61\3\1"+
    "\1\62\1\63\1\64";

  private static int [] zzUnpackAction() {
    int [] result = new int[171];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\65\0\152\0\237\0\324\0\u0109\0\u013e\0\u0173"+
    "\0\u01a8\0\u01dd\0\u0212\0\u0212\0\u0212\0\u0212\0\u0212\0\u0212"+
    "\0\u0212\0\u0212\0\u0247\0\u027c\0\u02b1\0\u02e6\0\u031b\0\u0350"+
    "\0\u0385\0\u03ba\0\u03ef\0\u0424\0\u0459\0\u048e\0\u04c3\0\u0212"+
    "\0\u0212\0\u0212\0\u0212\0\u04f8\0\u052d\0\u0562\0\u0597\0\u05cc"+
    "\0\u0601\0\u0636\0\u066b\0\u06a0\0\u06d5\0\u070a\0\u073f\0\u0212"+
    "\0\u0774\0\u07a9\0\u07de\0\u0813\0\u0848\0\u087d\0\u08b2\0\u08e7"+
    "\0\u091c\0\u0951\0\u0986\0\u09bb\0\65\0\u09f0\0\u0a25\0\u0a5a"+
    "\0\u0a8f\0\u0ac4\0\u0212\0\u0212\0\u0212\0\u0212\0\u0212\0\u0212"+
    "\0\u0636\0\u0af9\0\u066b\0\u0b2e\0\u0b63\0\u0b98\0\u0bcd\0\u013e"+
    "\0\u0c02\0\u0c37\0\u0c6c\0\u0636\0\u0ca1\0\u0cd6\0\u0212\0\u0d0b"+
    "\0\u0d40\0\u0d75\0\u0daa\0\u0ddf\0\u0e14\0\u0e49\0\u0e7e\0\u0eb3"+
    "\0\u0ee8\0\u0f1d\0\u0f52\0\65\0\u0f87\0\65\0\u0fbc\0\u0ff1"+
    "\0\u1026\0\u105b\0\u1090\0\u0bcd\0\u10c5\0\u10fa\0\u112f\0\u1164"+
    "\0\u1199\0\u0212\0\u11ce\0\u1203\0\u1238\0\u1164\0\u112f\0\u126d"+
    "\0\u12a2\0\u06d5\0\u12d7\0\u0d0b\0\u130c\0\u1341\0\u1376\0\u13ab"+
    "\0\u13e0\0\u1415\0\u144a\0\65\0\u147f\0\65\0\65\0\65"+
    "\0\65\0\u14b4\0\u14e9\0\u151e\0\u1553\0\u11ce\0\u1588\0\u11ce"+
    "\0\u1164\0\u0212\0\u0c37\0\u1238\0\u1238\0\u15bd\0\65\0\u15f2"+
    "\0\u1627\0\u165c\0\u1691\0\u16c6\0\65\0\u16fb\0\65\0\65"+
    "\0\65\0\u1730\0\65\0\65\0\65\0\u1765\0\u179a\0\u17cf"+
    "\0\65\0\65\0\65";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[171];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\0\1\2\1\3\1\4\1\5\2\6\1\7\1\0"+
    "\2\6\1\0\1\10\1\0\1\11\1\12\1\13\1\14"+
    "\1\15\1\16\1\17\1\20\1\21\1\22\1\23\1\24"+
    "\1\2\1\25\1\26\1\2\1\27\1\30\1\2\1\31"+
    "\1\32\1\2\1\33\1\2\1\34\1\35\3\2\1\36"+
    "\1\37\1\40\1\41\1\42\1\43\1\44\1\45\1\46"+
    "\1\47\1\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\23\2\13\0\4\3\6\0\1\3\3\0\1\3\10\0"+
    "\23\3\15\0\2\4\63\0\1\50\1\5\65\0\2\6"+
    "\2\0\2\6\52\0\5\7\1\51\1\52\1\53\1\54"+
    "\4\7\1\55\47\7\14\0\1\56\1\0\1\57\62\0"+
    "\1\60\51\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\4\2\1\61\16\2\100\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\1\2\1\62\11\2\1\63\7\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\4\2\1\64"+
    "\16\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\6\2\1\65\14\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\1\2\1\66\3\2\1\67\15\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\70\10\0\20\2\1\71"+
    "\2\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\4\2\1\72\15\2\1\73\13\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\7\2\1\74\6\2\1\75\4\2"+
    "\13\0\4\2\6\0\1\2\3\0\1\2\10\0\13\2"+
    "\1\76\7\2\13\0\4\2\6\0\1\2\3\0\1\77"+
    "\10\0\13\2\1\100\7\2\13\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\2\2\1\101\20\2\13\0\4\2"+
    "\6\0\1\2\3\0\1\2\10\0\20\2\1\102\2\2"+
    "\66\0\1\103\64\0\1\104\64\0\1\105\64\0\1\106"+
    "\73\0\1\107\65\0\1\110\3\0\2\50\60\0\6\111"+
    "\1\52\1\111\1\112\64\111\1\112\54\111\5\113\2\111"+
    "\1\113\1\114\4\113\1\111\47\113\5\115\1\116\1\117"+
    "\1\120\1\121\1\122\3\115\1\123\1\115\1\7\16\115"+
    "\2\7\25\115\5\55\1\51\1\52\1\124\1\125\54\55"+
    "\5\56\1\126\1\127\6\56\1\0\47\56\16\130\1\131"+
    "\46\130\1\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\6\2\1\132\14\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\2\2\1\133\20\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\7\2\1\134\13\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\7\2\1\135"+
    "\13\2\13\0\4\2\6\0\1\2\3\0\1\136\10\0"+
    "\2\2\1\137\20\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\3\2\1\140\17\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\6\2\1\141\14\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\22\2\1\142"+
    "\13\0\4\2\6\0\1\2\3\0\1\2\10\0\11\2"+
    "\1\143\11\2\13\0\4\2\6\0\1\2\3\0\1\2"+
    "\10\0\17\2\1\144\3\2\13\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\1\2\1\145\21\2\13\0\4\2"+
    "\6\0\1\2\3\0\1\2\10\0\6\2\1\146\14\2"+
    "\13\0\4\2\6\0\1\2\3\0\1\2\10\0\11\2"+
    "\1\147\11\2\13\0\4\2\6\0\1\2\3\0\1\2"+
    "\10\0\4\2\1\150\16\2\13\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\13\2\1\151\7\2\13\0\4\2"+
    "\6\0\1\2\3\0\1\2\10\0\1\2\1\152\21\2"+
    "\13\0\4\2\6\0\1\2\3\0\1\2\10\0\11\2"+
    "\1\153\11\2\12\0\7\154\1\111\7\154\1\111\16\154"+
    "\2\111\25\154\5\155\2\154\1\113\1\156\1\157\3\155"+
    "\1\154\1\155\1\113\16\155\2\113\25\155\5\160\1\161"+
    "\1\162\1\163\1\164\4\160\1\165\47\160\6\0\2\162"+
    "\64\0\1\162\55\0\5\160\1\161\1\162\1\166\1\164"+
    "\1\122\3\160\1\165\47\160\5\122\1\161\1\162\1\167"+
    "\1\170\4\122\1\165\47\122\5\165\1\161\2\162\1\171"+
    "\54\165\5\123\1\116\1\117\1\172\1\173\6\123\1\55"+
    "\16\123\2\55\25\123\6\0\1\127\56\0\16\174\1\175"+
    "\46\174\14\130\1\127\50\130\1\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\22\2\1\176\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\3\2\1\177\17\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\6\2\1\200"+
    "\14\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\15\2\1\201\5\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\11\2\1\202\11\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\6\2\1\203\14\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\4\2\1\204"+
    "\16\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\4\2\1\205\16\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\4\2\1\206\16\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\3\2\1\207\17\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\1\2\1\210"+
    "\21\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\10\2\1\211\12\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\2\2\1\212\20\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\1\2\1\213\21\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\3\2\1\214"+
    "\17\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\1\2\1\215\21\2\12\0\5\216\2\0\1\163\1\217"+
    "\4\216\1\0\54\216\2\0\1\163\1\217\1\157\3\216"+
    "\1\0\47\216\5\157\2\0\1\167\5\157\1\0\47\157"+
    "\5\160\1\161\1\162\1\220\1\164\4\160\1\165\47\160"+
    "\6\0\1\162\56\0\5\216\2\0\1\216\1\217\4\216"+
    "\1\0\47\216\5\160\1\161\1\162\1\221\1\164\1\122"+
    "\3\160\1\165\47\160\5\165\1\161\1\162\1\222\1\171"+
    "\54\165\5\122\1\161\1\162\1\223\1\170\4\122\1\165"+
    "\47\122\5\165\1\161\1\162\1\224\1\171\61\165\1\161"+
    "\1\162\1\225\1\171\54\165\14\174\1\127\50\174\1\0"+
    "\4\2\6\0\1\2\3\0\1\226\10\0\23\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\3\2\1\227"+
    "\17\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\11\2\1\230\11\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\6\2\1\231\14\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\7\2\1\232\13\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\11\2\1\233"+
    "\11\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\7\2\1\234\13\2\13\0\4\2\6\0\1\2\3\0"+
    "\1\2\10\0\21\2\1\235\1\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\4\2\1\236\16\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\4\2\1\237"+
    "\16\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\4\2\1\240\16\2\12\0\5\216\2\0\1\216\1\217"+
    "\1\157\3\216\1\0\47\216\1\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\7\2\1\241\13\2\13\0\4\2"+
    "\6\0\1\2\3\0\1\2\10\0\7\2\1\242\13\2"+
    "\13\0\4\2\6\0\1\2\3\0\1\2\10\0\20\2"+
    "\1\243\2\2\13\0\4\2\6\0\1\2\3\0\1\2"+
    "\10\0\15\2\1\244\5\2\13\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\1\245\22\2\13\0\4\2\6\0"+
    "\1\2\3\0\1\2\10\0\10\2\1\246\12\2\13\0"+
    "\4\2\6\0\1\2\3\0\1\2\10\0\2\2\1\247"+
    "\20\2\13\0\4\2\6\0\1\2\3\0\1\2\10\0"+
    "\22\2\1\250\13\0\4\2\6\0\1\2\3\0\1\2"+
    "\10\0\3\2\1\251\17\2\13\0\4\2\6\0\1\2"+
    "\3\0\1\2\10\0\7\2\1\252\13\2\13\0\4\2"+
    "\6\0\1\2\3\0\1\2\10\0\4\2\1\253\16\2"+
    "\12\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[6148];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\1\0\5\1\1\0\3\1\10\11\15\1\4\11\2\1"+
    "\2\0\4\1\4\0\1\11\22\1\6\11\5\0\3\1"+
    "\3\0\1\1\1\0\1\1\1\11\1\1\1\0\22\1"+
    "\5\0\1\1\1\11\1\1\2\0\2\1\2\0\1\1"+
    "\3\0\20\1\2\0\2\1\1\11\31\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[171];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn;

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;
  
  /** 
   * The number of occupied positions in zzBuffer beyond zzEndRead.
   * When a lead/high surrogate has been read from the input stream
   * into the final zzBuffer position, this will have a value of 1;
   * otherwise, it will have a value of 0.
   */
  private int zzFinalHighSurrogate = 0;

  /* user code: */
	public int getLineNumber() { return yyline+1; }
	public int getColumnNumber() { return yycolumn+1; }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public Lexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x110000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 172) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      zzEndRead += zzFinalHighSurrogate;
      zzFinalHighSurrogate = 0;
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead);

      /* translate stored positions */
      zzEndRead-= zzStartRead;
      zzCurrentPos-= zzStartRead;
      zzMarkedPos-= zzStartRead;
      zzStartRead = 0;
    }

    /* is the buffer big enough? */
    if (zzCurrentPos >= zzBuffer.length - zzFinalHighSurrogate) {
      /* if not: blow it up */
      char newBuffer[] = new char[zzBuffer.length*2];
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
      zzBuffer = newBuffer;
      zzEndRead += zzFinalHighSurrogate;
      zzFinalHighSurrogate = 0;
    }

    /* fill the buffer with new input */
    int requested = zzBuffer.length - zzEndRead;           
    int totalRead = 0;
    while (totalRead < requested) {
      int numRead = zzReader.read(zzBuffer, zzEndRead + totalRead, requested - totalRead);
      if (numRead == -1) {
        break;
      }
      totalRead += numRead;
    }

    if (totalRead > 0) {
      zzEndRead += totalRead;
      if (totalRead == requested) { /* possibly more input available */
        if (Character.isHighSurrogate(zzBuffer[zzEndRead - 1])) {
          --zzEndRead;
          zzFinalHighSurrogate = 1;
        }
      }
      return false;
    }

    // totalRead = 0: End of stream
    return true;
  }

    
  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * Internal scan buffer is resized down to its initial length, if it has grown.
   *
   * @param reader   the new input stream 
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEOFDone = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = 0;
    zzFinalHighSurrogate = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
    if (zzBuffer.length > ZZ_BUFFERSIZE)
      zzBuffer = new char[ZZ_BUFFERSIZE];
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final String yytext() {
    return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
  }


  /**
   * Returns the character at position <tt>pos</tt> from the 
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer[zzStartRead+pos];
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) throws LexicalError {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new LexicalError(message);
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  throws LexicalError {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() throws java.io.IOException {
    if (!zzEOFDone) {
      zzEOFDone = true;
      yyclose();
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public Token next_token() throws java.io.IOException, LexicalError {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      boolean zzR = false;
      int zzCh;
      int zzCharCount;
      for (zzCurrentPosL = zzStartRead  ;
           zzCurrentPosL < zzMarkedPosL ;
           zzCurrentPosL += zzCharCount ) {
        zzCh = Character.codePointAt(zzBufferL, zzCurrentPosL, zzMarkedPosL);
        zzCharCount = Character.charCount(zzCh);
        switch (zzCh) {
        case '\u000B':
        case '\u000C':
        case '\u0085':
        case '\u2028':
        case '\u2029':
          yyline++;
          yycolumn = 0;
          zzR = false;
          break;
        case '\r':
          yyline++;
          yycolumn = 0;
          zzR = true;
          break;
        case '\n':
          if (zzR)
            zzR = false;
          else {
            yyline++;
            yycolumn = 0;
          }
          break;
        default:
          zzR = false;
          yycolumn += zzCharCount;
        }
      }

      if (zzR) {
        // peek one character ahead if it is \n (if we have counted one line too much)
        boolean zzPeek;
        if (zzMarkedPosL < zzEndReadL)
          zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        else if (zzAtEOF)
          zzPeek = false;
        else {
          boolean eof = zzRefill();
          zzEndReadL = zzEndRead;
          zzMarkedPosL = zzMarkedPos;
          zzBufferL = zzBuffer;
          if (eof) 
            zzPeek = false;
          else 
            zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        }
        if (zzPeek) yyline--;
      }
      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
  
      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {
    
          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 1: 
          { return new Token(sym.IDENTIFIER,yyline+1,yycolumn+1,"ID",yytext());
          }
        case 53: break;
        case 2: 
          { return new Token(sym.CLASS_ID,yyline+1,yycolumn+1,"CLASS_ID",yytext());
          }
        case 54: break;
        case 3: 
          { return new Token(sym.INTEGER, yyline+1,yycolumn+1,"INTEGER",yytext());
          }
        case 55: break;
        case 4: 
          { 
          }
        case 56: break;
        case 5: 
          { return new Token(sym.OP_DIV,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 57: break;
        case 6: 
          { return new Token(sym.OP_MULT,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 58: break;
        case 7: 
          { return new Token(sym.LPAREN,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 59: break;
        case 8: 
          { return new Token(sym.RPAREN,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 60: break;
        case 9: 
          { return new Token(sym.LBRACK,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 61: break;
        case 10: 
          { return new Token(sym.RBRACK,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 62: break;
        case 11: 
          { return new Token(sym.LBRACE,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 63: break;
        case 12: 
          { return new Token(sym.RBRACE,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 64: break;
        case 13: 
          { return new Token(sym.SEMICOLON,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 65: break;
        case 14: 
          { return new Token(sym.COMMA, yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 66: break;
        case 15: 
          { return new Token(sym.OP_NOT,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 67: break;
        case 16: 
          { return new Token(sym.OP_ASSIGNMENT,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 68: break;
        case 17: 
          { return new Token(sym.OP_DOT,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 69: break;
        case 18: 
          { return new Token(sym.OP_MINUS,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 70: break;
        case 19: 
          { return new Token(sym.OP_MOD,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 71: break;
        case 20: 
          { return new Token(sym.OP_ADD,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 72: break;
        case 21: 
          { return new Token(sym.OP_LT,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 73: break;
        case 22: 
          { return new Token(sym.OP_GT,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 74: break;
        case 23: 
          { throw new LexicalError("illegal integer " + yytext() + " at " + (yyline+1) + ":" + (yycolumn+1));
          }
        case 75: break;
        case 24: 
          { throw new LexicalError("illegal string " + yytext().trim() + " at " + (yyline+1) + ":" + (yycolumn+1));
          }
        case 76: break;
        case 25: 
          { String str = yytext();
			char[] escapes = { 9, /*tab*/
							   10, /* line feeder */
							   13 /* carriage return */ };
			str = str.replace("\\t", escapes[0] + "");
			str = str.replace("\\n", escapes[1] + "");
			str = str.replace("\\r", escapes[2] + "");
			return new Token(sym.STR,yyline+1,yycolumn+1,"STRING",str);
          }
        case 77: break;
        case 26: 
          { throw new LexicalError("comment closure at " + (yyline+1) + ":" + (yycolumn+1) + " has no beginning (not a legal comment).");
          }
        case 78: break;
        case 27: 
          { return new Token(sym.IF,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 79: break;
        case 28: 
          { return new Token(sym.OP_NEQ,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 80: break;
        case 29: 
          { return new Token(sym.OP_EQ,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 81: break;
        case 30: 
          { return new Token(sym.OP_LEQ,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 82: break;
        case 31: 
          { return new Token(sym.OP_GEQ,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 83: break;
        case 32: 
          { return new Token(sym.OP_AND,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 84: break;
        case 33: 
          { return new Token(sym.OP_OR,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 85: break;
        case 34: 
          { throw new LexicalError("comment starting at " + (yyline+1) + ":" + (yycolumn+1) + " is missing closure '*/'");
          }
        case 86: break;
        case 35: 
          { return new Token(sym.NEW,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 87: break;
        case 36: 
          { return new Token(sym.INT,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 88: break;
        case 37: 
          { return new Token(sym.ELSE,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 89: break;
        case 38: 
          { return new Token(sym.TRUE,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 90: break;
        case 39: 
          { return new Token(sym.THIS,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 91: break;
        case 40: 
          { return new Token(sym.NULL,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 92: break;
        case 41: 
          { return new Token(sym.VOID,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 93: break;
        case 42: 
          { return new Token(sym.CLASS,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 94: break;
        case 43: 
          { return new Token(sym.BREAK,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 95: break;
        case 44: 
          { return new Token(sym.FALSE,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 96: break;
        case 45: 
          { return new Token(sym.WHILE,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 97: break;
        case 46: 
          { return new Token(sym.RETURN,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 98: break;
        case 47: 
          { return new Token(sym.LENGTH,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 99: break;
        case 48: 
          { return new Token(sym.STRING,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 100: break;
        case 49: 
          { return new Token(sym.STATIC,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 101: break;
        case 50: 
          { return new Token(sym.EXTENDS,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 102: break;
        case 51: 
          { return new Token(sym.BOOLEAN,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 103: break;
        case 52: 
          { return new Token(sym.CONTINUE,yyline+1,yycolumn+1,yytext(),yytext());
          }
        case 104: break;
        default: 
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
            zzDoEOF();
              { 	return new Token(sym.EOF, yyline+1,yycolumn+1,yytext(),yytext());
 }
          } 
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
