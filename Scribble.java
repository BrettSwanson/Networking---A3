/* Game thread for the Scribble app

   @author YOUR FULL NAME GOES HERE

   @version CS 391 - Spring 2018 - A3
*/

import java.util.*;
import java.io.*;
import java.net.*;

class Scribble implements Runnable {
    static char[] tiles = {'A','A','A','A','A','A','A','A','A',
        'B','B','C','C','D','D','D','D',
        'E','E','E','E','E','E','E','E','E','E','E','E',
        'F','F','G','G','G','H','H','I','I','I','I','I','I','I','I','I',
        'J','K','L','L','L','L','M','M','N','N','N','N','N','N',
        'O','O','O','O','O','O','O','O','P','P','Q','R','R','R','R','R','R',
        'S','S','S','S','T','T','T','T','T','T',
        'U','U','U','U','V','V','W','W','X','Y','Y','Z'};
    static final int WIDTH = 10;      // width of board
    static final int HEIGHT = 10;     // height of board
    static final int MAX_TURNS = 3;   // number of turns in a game
    static String[] dict;             // dictionary of allowed words
    char[][] board;                   // the Scribble board
    State state;                      // current state in the Scribble FSM
    Socket player1, player2;          // player sockets
    DataInputStream in1, in2;         // input streams of player sockets
    DataOutputStream out1, out2;      // output streams of player sockets
    String name1, name2;              // player names
    int turn;                         // the current turn in the game
    char[] rack1, rack2;              // tile rack for both players
    int score1, score2;               // scores for both players
    Random rnd;                       // random generator for the whole game

    /* add your instance/class variables, if any, after this point and before
       the constructor that follows */


    /* initialize a Scribble game:
       + load the dictionary
       + open the sockets' streams
       + create an empty board
       + initialize the two racks with 7 random tiles each
       + initialize other variables, as needed, including rnd with the given
         seed
     */
    Scribble(Socket clientSocket1, Socket clientSocket2, int seed) {
      loadDictionary();
      board = buildBoard();
      this.player1 = clientSocket1;
      this.player2 = clientSocket2;

      openStreams(player1, player2);

      rnd = new Random(seed);
    }// constructor


    /* implement the Scribble FSM given in the handout. The output
       sent to the console by this method is also specified in the handout,
       namely in the provided traces.
     */
    public void run() {
        try {
            startGame();
        } catch(EOFException e) {
            System.out.println("Client died unexpectedly");
        }catch (IOException e) {
            System.out.println("blah blah blah " + e);
        }

    }// run method

    /* return the string representation of the current game state from the
       perspective of the given player (i.e., 1 or 2). More precisely, the
       returned string must contain, in order:
       + the state of the board
       + the turn number
       + the scores of both players
       + the rack of the given player
       The format of the returned string is fully specified in the traces
       included in the handout.
     */
    String getGameState(int player) {
    	String boardString = toString();	
    	String playerTurn = "Turn: ";
    	if (player == 1){
    		playerTurn += ((Integer)((turn/2)+1)).toString();
    	} else {
    		playerTurn += ((Integer)(turn/2)).toString();
    	}
    	String scores = "";
    	if (player == 1){
    		scores = "Scores: " + ((Integer)score1).toString()
    			   + " (opponent: " + ((Integer)score2).toString() + ")";
    	} else {
    		scores = "Scores: " + ((Integer)score2).toString()
     			   + " (opponent: " + ((Integer)score1).toString() + ")";
    	}
    	String rack = "Rack: ";
    	if (player == 1){
    		for (int i = 0; i < rack1.length; i++){
    			rack += rack1[i];
    		}
    	} else {
    		for (int i = 0; i < rack2.length; i++){
    			rack += rack2[i];
    		}
    	}
      // To be completed

      return boardString + "\n" + playerTurn + "\n" 
      			+ scores + "\n" + rack + "\n";
    }// getGameState method

    /* Initialize dict with the contents of the dict.txt file (stored in the
       same directory as this file).
       Each word must be in all uppercase. Duplicates (if any) must be
       removed, yielding a total of 276,643 distinct words that must be
       sorted in alphabetical order within dict.
     */
     static void loadDictionary() {
         dict = new String[276643];
     try {
         File file = new File("dict.txt");
         FileReader fileReader =  new FileReader(file);
         BufferedReader bufferedReader = new BufferedReader(fileReader);
         String line;
         String prevLine = "";
         int lineNumber = 0;
         while ((line = bufferedReader.readLine()) != null) {
             if (!prevLine.equals(line)) {
                 dict[lineNumber] = line;
                 lineNumber++;
             }
             prevLine = line;
         }
         fileReader.close();
     } catch (IOException e) {
         e.printStackTrace();
     }

     }// loadDictionary method

    /* return true if and only if the given word (assumed to be all uppercase)
       is in dict
     */
    static boolean isInDictionary(String word) {
      return Arrays.asList(dict).contains(word.toUpperCase());
    }// isInDictionary method

    /* convert the Scribble board to a string
       The format of this string is fully specified in the traces included
       in the handout. Here is what the empty board must look like:

        |0|1|2|3|4|5|6|7|8|9|
       -+-+-+-+-+-+-+-+-+-+-+
       A| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       B| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       C| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       D| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       E| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       F| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       G| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       H| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       I| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+
       J| | | | | | | | | | |
       -+-+-+-+-+-+-+-+-+-+-+

       The traces in the handout also show that some of the vertical and
       horizontal bars must be omitted, namely when they appear between
       two letters. Similarly, an interior '+' may NOT be displayed when it
       is surrounded by 4 letters.
     */
    public String toString() {
    	String boardString = "";
    	for (int i = 0; i < board.length; i++){ 
    		for (int j = 0; j < board[i].length; j++){
    			if (i > 2 && j > 2 && i < 20 && j < 20 && i%2 != 0 && j%2 != 0){
    				boolean tl = false;
    				if (board[i-1][j-1] != ' '){
    				    tl = true;
    				}
    				boolean tr = false;
    				if (board[i-1][j+1] != ' '){
    				    tr = true;
    				}
    				boolean bl = false;
    				if (board[i+1][j-1] != ' '){
    				    bl = true;
    				}
    				boolean br = false;
    				if (board[i+1][j+1] != ' '){
    				    br = true;
    				}
    				if (tl && tr && bl && br){
    					boardString += " ";
    				} else {
    					boardString += "+";
    				}
    				
    			}
    			boardString += board[i][j];
    		}
    		boardString += "\n";
    	}
      // To be completed

      return boardString;
    }// toString method

  /* open the I/O streams of the given sockets and assign them to the
     corresponding instance variables of this object
   */
  void openStreams(Socket socket1, Socket socket2) {

      try {
          in1 = new DataInputStream(player1.getInputStream());
          in2 = new DataInputStream(player2.getInputStream());
          out1 = new DataOutputStream(player1.getOutputStream());
          out2 = new DataOutputStream(player2.getOutputStream());
      } catch (IOException e) {
          System.out.println(e.getMessage());
      }

  }// openStreams method

  /* states of the Scribble FSM. You MUST use this data type, e.g., State.I1

     *** do NOT modify this enum type ***
   */
  public enum State { I1, I2, I3, I4, I5, I6, I7, I8 }

  /* exception that must be raised when a player puts down an invalid word,
     that is, a word w that meets at least one of the following requirements:
     + at least one of the words formed using w is not in the dictionary
     + w is too long to fit on the board, i.e., it goes off the right
       side of the board when placed in the 'across' direction or it goes off
       the bottom of the board when placed in the 'down' direction
     + one of the letters in w does not match an existing letter already on the
       board at that position
     + one of the letters in w is not on the player's rack and the position of
       that letter on the board is empty (i.e., w is not reusing an existing
       letter on the board)
     + w does not build on an existing word and this is not the first word of
       the game

     You must instantiate this exception class in your solution whenever
     appropriate.

     *** do NOT modify this class ***
   */
  class BadWordPlacementException extends RuntimeException {
      BadWordPlacementException(String message) {
          super(message);
      }
  }// BadWordPlacementException class

    /* add your instance methods after this point */

    public char[][] buildBoard() {
      char[][] dummyBoard = new char[22][22];
      String stringBoard = " |0|1|2|3|4|5|6|7|8|9|" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "A| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "B| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "C| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "D| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "E| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "F| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "G| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "H| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "I| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+" +
              "J| | | | | | | | | | |" +
              "-+-+-+-+-+-+-+-+-+-+-+";
      int curr = 0;
      for (int i = 0; i < 22; i++) {
          for(int j = 0; j < 22; j++) {
              dummyBoard[i][j] = stringBoard.charAt(curr);
              curr++;
          }
      }
       return dummyBoard;
    }

    public void startGame() throws IOException {
        boolean first = rnd.nextBoolean();
    }



}// Scribble class
