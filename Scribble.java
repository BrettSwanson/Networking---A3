/* Game thread for the Scribble app

   @author Brett Swanson and Trevor Nipko

   @version CS 391 - Spring 2018 - A3
*/

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

class Scribble implements Runnable {
    static char[] tiles = {'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
            'B', 'B', 'C', 'C', 'D', 'D', 'D', 'D',
            'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E',
            'F', 'F', 'G', 'G', 'G', 'H', 'H', 'I', 'I', 'I', 'I', 'I', 'I', 'I', 'I', 'I',
            'J', 'K', 'L', 'L', 'L', 'L', 'M', 'M', 'N', 'N', 'N', 'N', 'N', 'N',
            'O', 'O', 'O', 'O', 'O', 'O', 'O', 'O', 'P', 'P', 'Q', 'R', 'R', 'R', 'R', 'R', 'R',
            'S', 'S', 'S', 'S', 'T', 'T', 'T', 'T', 'T', 'T',
            'U', 'U', 'U', 'U', 'V', 'V', 'W', 'W', 'X', 'Y', 'Y', 'Z'};
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
    DataOutputStream fpOut, spOut;
    DataInputStream spIn, fpIn;
    String waitMessage = ", please wait for your opponent...\n";
    String sPrompt = "Start location of your word (e.g., B3)? ";
    String dPrompt = "Direction of your word (A or D): ";
    String wPrompt = "Your word: ";
    String winGameOver = "You won - GAME OVER!";
    String lostGameOver = "You lost - GAME OVER!";
    boolean gameOver = false;
    String currStart;
    String currDirection;
    boolean firstWord = true;
    char[][] checkBoard = new char[22][22];

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
        openStreams(clientSocket1, clientSocket2);
        state = State.I1;
        rnd = new Random(seed);
        turn = 1;
        this.rack1 = new char[7];
        for (int i = 0; i < rack1.length; i++) {
            rack1[i] = tiles[rnd.nextInt(tiles.length)];
        }
        this.rack2 = new char[7];
        for (int i = 0; i < rack2.length; i++) {
            rack2[i] = tiles[rnd.nextInt(tiles.length)];
        }
    }// constructor

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
            FileReader fileReader = new FileReader(file);
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

    /* implement the Scribble FSM given in the handout. The output
       sent to the console by this method is also specified in the handout,
       namely in the provided traces.
     */
    public void run() {
        try {
            startGame();
        } catch (EOFException e) {
            System.out.println("Client died unexpectedly");
        } catch (IOException e) {
            System.out.println("I/O Error: " + e);
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
        String playerTurn = "Turn: " + turn;
        String scores = "";
        if (player == 1) {
            scores = "Scores: " + ((Integer) score1).toString()
                    + " (opponent: " + ((Integer) score2).toString() + ")";
        } else {
            scores = "Scores: " + ((Integer) score2).toString()
                    + " (opponent: " + ((Integer) score1).toString() + ")";
        }
        String rack = "Rack: ";
        if (player == 1) {
            for (int i = 0; i < rack1.length; i++) {
                rack += rack1[i];
            }
        } else {
            for (int i = 0; i < rack2.length; i++) {
                rack += rack2[i];
            }
        }
        // To be completed

        return boardString + "\n" + playerTurn + "\n"
                + scores + "\n" + rack + "\n";
    }// getGameState method

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
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (i > 2 && j > 2 && i < 20 && j < 20 && i % 2 != 0 && j % 2 != 0) {
                    boolean tl = false;
                    if (board[i - 1][j - 1] != ' ') {
                        tl = true;
                    }
                    boolean tr = false;
                    if (board[i - 1][j + 1] != ' ') {
                        tr = true;
                    }
                    boolean bl = false;
                    if (board[i + 1][j - 1] != ' ') {
                        bl = true;
                    }
                    boolean br = false;
                    if (board[i + 1][j + 1] != ' ') {
                        br = true;
                    }
                    if (tl && tr && bl && br) {
                        board[i][j] = ' ';
                        board[i - 1][j] = ' ';
                        board[i + 1][j] = ' ';
                        board[i][j + 1] = ' ';
                        board[i][j - 1] = ' ';
                    }

                }
                if (i > 2 && j > 2 && i < 20 && j < 20 && i % 2 == 0 && j % 2 == 0) {
                    if (board[i][j + 2] != ' ' && board[i][j] != ' ') {
                        board[i][j + 1] = ' ';
                    }
                    if (board[i + 2][j] != ' ' && board[i][j] != ' ') {
                        board[i + 1][j] = ' ';
                    }
                }
            }

        }


        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                boardString += board[i][j];
            }
            if (i != board.length - 1) {
                boardString += "\n";
            }
        }


        // To be completed

        return boardString;
    }// toString method

    /* open the I/O streams of the given sockets and assign them to the
       corresponding instance variables of this object
     */
    void openStreams(Socket socket1, Socket socket2) {

        try {
            in1 = new DataInputStream(socket1.getInputStream());
            in2 = new DataInputStream(socket2.getInputStream());
            out1 = new DataOutputStream(socket1.getOutputStream());
            out2 = new DataOutputStream(socket2.getOutputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }// openStreams method

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
        String stringBoardCheck = " |0|1|2|3|4|5|6|7|8|9|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "A|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "B|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "C|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "D|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "E|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "F|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "G|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "H|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "I|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+" +
                "J|0|0|0|0|0|0|0|0|0|0|" +
                "-+-+-+-+-+-+-+-+-+-+-+";
        int curr = 0;
        for (int i = 0; i < 22; i++) {
            for (int j = 0; j < 22; j++) {
                dummyBoard[i][j] = stringBoard.charAt(curr);
                checkBoard[i][j] = stringBoardCheck.charAt(curr);
                curr++;
            }
        }
        return dummyBoard;
    }

    public void startGame() throws IOException {
        String reply;
        boolean first = rnd.nextBoolean();
        if (first) {
            fpOut = out2;
            fpIn = in2;
            spOut = out1;
            spIn = in1;
        } else {
            fpOut = out1;
            fpIn = in1;
            spOut = out2;
            spIn = in2;
        }
        fpOut.writeUTF("Enter your name: ");
        state = State.I1;
        boolean formatCorrect;
        while (true) {
            switch (state) {
                case I1:
                    name1 = fpIn.readUTF();
                    fpOut.writeUTF(name1 + waitMessage + "\n");
                    spOut.writeUTF("Enter your name: ");
                    state = State.I2;
                    break;
                case I2:
                    name2 = spIn.readUTF();
                    spOut.writeUTF(name2 + waitMessage);
                    fpOut.writeUTF(getGameState(1) + sPrompt);
                    state = State.I3;
                    break;
                case I3:
                    reply = fpIn.readUTF();
                    formatCorrect = false;
                    if (((int) reply.charAt(0) >= 65) && (int) reply.charAt(0) <= 74) {
                        if (((int) reply.charAt(1) >= 48) && (int) reply.charAt(1) <= 57) {
                            formatCorrect = true;
                        }
                    }
                    if (!formatCorrect) {
                        fpOut.writeUTF(getGameState(1) + "Invalid location!\n" + sPrompt);
                        break;
                    } else {
                        currStart = reply;
                        fpOut.writeUTF(dPrompt);
                        state = State.I4;
                        break;
                    }
                case I4:
                    reply = fpIn.readUTF();
                    formatCorrect = false;
                    if (reply.equalsIgnoreCase("A") || reply.equalsIgnoreCase("D")) {
                        formatCorrect = true;
                    }

                    if (formatCorrect) {
                        currDirection = reply;
                        fpOut.writeUTF(wPrompt);
                        state = State.I5;
                        break;
                    } else {
                        fpOut.writeUTF("Invalid direction!\n" + dPrompt);
                        break;
                    }
                case I5:
                    reply = fpIn.readUTF();
                    try {
                        isValidWord(reply, 1);
                        fpOut.writeUTF(getGameState(1) + "\n" + name1 + waitMessage);
                        spOut.writeUTF(getGameState(2) + sPrompt);
                        state = State.I6;
                        break;
                    } catch (BadWordPlacementException e) {
                        fpOut.writeUTF(getGameState(1) + e.getMessage() + "\n" + name1 + waitMessage);
                        spOut.writeUTF(getGameState(2) + sPrompt);
                        state = State.I6;
                        break;
                    }
                case I6:
                    reply = spIn.readUTF();
                    formatCorrect = false;
                    if (((int) reply.charAt(0) >= 65) && (int) reply.charAt(0) <= 74) {
                        if (((int) reply.charAt(1) >= 48) && (int) reply.charAt(1) <= 57) {
                            formatCorrect = true;
                        }
                    }
                    if (!formatCorrect) {
                        spOut.writeUTF("Invalid location!\n" + sPrompt);
                        break;
                    } else {
                        currStart = reply;
                        spOut.writeUTF(dPrompt);
                        state = State.I7;
                        break;
                    }
                case I7:
                    reply = spIn.readUTF();
                    formatCorrect = false;
                    if (reply.equalsIgnoreCase("A") || reply.equalsIgnoreCase("D")) {
                        formatCorrect = true;
                    }

                    if (formatCorrect) {
                        currDirection = reply;
                        spOut.writeUTF(wPrompt);
                        state = State.I8;
                        break;
                    } else {
                        spOut.writeUTF("Invalid direction!\n" + dPrompt);
                        break;
                    }
                case I8:
                    reply = spIn.readUTF();
                    try {
                        isValidWord(reply, 2);
                        if (turn == MAX_TURNS) {
                            if (score1 > score2) {
                                fpOut.writeUTF(getGameState(1) + winGameOver);
                                spOut.writeUTF(getGameState(2) + lostGameOver);
                            } else {
                                fpOut.writeUTF(getGameState(1) + lostGameOver);
                                spOut.writeUTF(getGameState(2) + winGameOver);
                            }
                            gameOver = true;
                            break;
                        } else {
                            turn++;
                            spOut.writeUTF(getGameState(2) + name2 + waitMessage);
                            fpOut.writeUTF(getGameState(1) + sPrompt);
                            state = State.I3;
                            break;
                        }
                    } catch (BadWordPlacementException e) {
                        if (turn == MAX_TURNS) {
                            if (score1 > score2) {
                                fpOut.writeUTF(getGameState(1) + winGameOver);
                                spOut.writeUTF(getGameState(2) + e.getMessage() + "\n" + lostGameOver);
                            } else {
                                fpOut.writeUTF(getGameState(1) + lostGameOver);
                                spOut.writeUTF(getGameState(2) + e.getMessage() + "\n" + winGameOver);
                            }
                            gameOver = true;
                        } else {
                            turn++;
                            spOut.writeUTF(getGameState(2) + e.getMessage() + "\n" + name2 + waitMessage);
                            fpOut.writeUTF(getGameState(1) + sPrompt);
                            state = State.I3;
                            break;
                        }
                    }

            }
            if (gameOver) {
                break;
            }
        }
        close();
    }

    /* add your instance methods after this point */

    public void isValidWord(String word, int player) {
        int startRow = 2 * (currStart.charAt(0) - 65) + 2;
        int startCol = 2 * (Character.getNumericValue(currStart.charAt(1))) + 2;
        String tempWord = "";
        int tempWordStart;
        int tempWordEnd;
        int tempRow = startRow;
        int tempCol = startCol;
        char[][] tempBoard = new char[22][22];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                tempBoard[i][j] = board[i][j];
            }
        }
        boolean hitsExisting = false;
        boolean onRack = false;
        char[] tempRack;
        int tempScore;
        boolean scoreCheck = false;

        if (player == 1) {
            tempRack = new char[rack1.length];
            for (int i = 0; i < rack1.length; i++) {
                tempRack[i] = rack1[i];
            }
            tempScore = score1;
        } else {
            tempRack = new char[rack2.length];
            for (int i = 0; i < rack1.length; i++) {
                tempRack[i] = rack2[i];
            }
            tempScore = score2;
        }

        for (int i = 0; i < 22; i++) {
            for (int j = 0; j < 22; j++) {
                tempBoard[i][j] = board[i][j];
            }
        }

        if (!isInDictionary(word)) {
            throw new BadWordPlacementException("The word " + word + " is not in the dictionary.");
        } else {
            tempScore += word.length();
        }

        if (currDirection.equals("A")) {
            for (int i = 0; i < word.length(); i++) {
                if (tempCol > 20) {
                    throw new BadWordPlacementException(word + " is too long to fit on the board.");

                } else {
                    if (tempBoard[tempRow][tempCol] != ' ') {
                        if (tempBoard[tempRow][tempCol] != word.charAt(i)) {
                            throw new BadWordPlacementException(word.charAt(i) + " in " + word + " conflicts with a different letter on the board.");
                        }
                        hitsExisting = true;
                    } else {
                        for (int j = 0; j < tempRack.length; j++) {
                            if (word.charAt(i) == tempRack[j]) {
                                onRack = true;
                                tempRack[j] = ' ';
                                break;
                            }
                        }
                        if (!onRack) {
                            throw new BadWordPlacementException("You do not have the letter " + word.charAt(i) + " on your rack!");
                        }
                    }
                    tempBoard[tempRow][tempCol] = word.charAt(i);
                    tempCol += 2;
                    onRack = false;
                }
            }

            tempCol = startCol;
            for (int i = 0; i < word.length(); i++) {
                tempRow = startRow;
                tempWordStart = tempRow;
                if ((tempRow - 2 >= 2 && tempBoard[tempRow - 2][tempCol] != ' ') || (tempRow + 2 <= 20 && tempBoard[tempRow + 2][tempCol] != ' ')) {
                    while (tempRow - 2 >= 2 && tempBoard[tempRow - 2][tempCol] != ' ') {
                        tempWordStart -= 2;
                        tempRow -= 2;
                    }
                    tempRow = startRow;
                    tempWordEnd = tempRow;
                    while (tempRow + 2 <= 20 && tempBoard[tempRow + 2][tempCol] != ' ') {
                        tempWordEnd += 2;
                        tempRow += 2;
                    }

                    for (int j = tempWordStart; j <= tempWordEnd; j += 2) {
                        tempWord += tempBoard[j][tempCol];
                        if (checkBoard[j][tempCol] == '0') {
                            scoreCheck = true;
                        }
                        if (checkBoard[j][tempCol] == '1') {
                            hitsExisting = true;
                        }
                    }

                    if (!isInDictionary(tempWord)) {
                        throw new BadWordPlacementException("The word " + tempWord + " is not in the dictionary.");
                    } else {
                        if (scoreCheck) {
                            tempScore += tempWord.length();
                        }
                        tempWord = "";
                        scoreCheck = false;
                    }

                }
                tempCol += 2;
            }
        } else {
            for (int i = 0; i < word.length(); i++) {
                if (tempRow > 20) {
                    throw new BadWordPlacementException(word + " is too long to fit on the board.");
                } else {
                    if (tempBoard[tempRow][tempCol] != ' ') {
                        if (tempBoard[tempRow][tempCol] != word.charAt(i)) {
                            throw new BadWordPlacementException(word.charAt(i) + " in " + word + " conflicts with a different letter on the board.");
                        }
                        hitsExisting = true;
                    } else {
                        for (int j = 0; j < tempRack.length; j++) {
                            if (word.charAt(i) == tempRack[j]) {
                                onRack = true;
                                tempRack[j] = ' ';
                                break;
                            }
                        }
                        if (!onRack) {
                            throw new BadWordPlacementException("You do not have the letter " + word.charAt(i) + " on your rack!");
                        }
                    }
                    tempBoard[tempRow][tempCol] = word.charAt(i);
                    tempRow += 2;
                    onRack = false;
                }
            }

            tempRow = startRow;
            for (int i = 0; i < word.length(); i++) {
                tempCol = startCol;
                tempWordStart = tempCol;
                if ((tempCol - 2 >= 2 && tempBoard[tempRow][tempCol - 2] != ' ') || (tempCol + 2 <= 20 && tempBoard[tempRow][tempCol + 2] != ' ')) {
                    while (tempCol - 2 >= 2 && tempBoard[tempRow][tempCol - 2] != ' ') {
                        tempWordStart -= 2;
                        tempCol -= 2;
                    }
                    tempCol = startCol;
                    tempWordEnd = tempCol;
                    while (tempCol + 2 <= 20 && tempBoard[tempRow][tempCol + 2] != ' ') {
                        tempWordEnd += 2;
                        tempCol += 2;
                    }

                    for (int j = tempWordStart; j <= tempWordEnd; j += 2) {
                        tempWord += tempBoard[tempRow][j];
                        if (checkBoard[tempRow][j] == '0') {
                            scoreCheck = true;
                        }
                        if (checkBoard[tempRow][j] == 'i') {
                            hitsExisting = true;
                        }
                    }

                    if (!isInDictionary(tempWord)) {
                        throw new BadWordPlacementException("The word " + tempWord + " is not in the dictionary.");
                    } else {
                        if (scoreCheck) {
                            tempScore += tempWord.length();
                        }
                        tempWord = "";
                        scoreCheck = false;
                    }

                }
                tempRow += 2;
            }
        }
        if (!hitsExisting) {
            if (firstWord) {
                firstWord = false;
            } else {
                throw new BadWordPlacementException(word + " does not build on an existing word.");
            }
        }
        tempRow = startRow;
        tempCol = startCol;
        if (currDirection.equals("A")) {
            for (int i = 0; i < word.length(); i++) {
                checkBoard[tempRow][tempCol] = '1';
                tempCol += 2;
            }
        } else {
            for (int i = 0; i < word.length(); i++) {
                checkBoard[tempRow][tempCol] = '1';
                tempRow += 2;
            }
        }
        board = tempBoard;
        if (player == 1) {
            score1 = tempScore;
            for (int i = 0; i < tempRack.length; i++) {
                if (tempRack[i] == ' ') {
                    tempRack[i] = tiles[rnd.nextInt(tiles.length)];
                }
            }
            rack1 = tempRack;
        } else {
            score2 = tempScore;
            for (int i = 0; i < tempRack.length; i++) {
                if (tempRack[i] == ' ') {
                    tempRack[i] = tiles[rnd.nextInt(tiles.length)];
                }
            }
            rack2 = tempRack;
        }
    }

    void close() {
        try {
            if (in1 != null) {
                in1.close();
            }
            if (in2 != null) {
                in2.close();
            }
            if (out1 != null) {
                out1.close();
            }
            if (out2 != null) {
                out2.close();
            }
            if (player1 != null) {
                player1.close();
            }
            if (player2 != null) {
                player2.close();
            }
        } catch (IOException e) {
            System.err.println("Error in close(): " +
                    e.getMessage());
        }

    }// close method

    /* states of the Scribble FSM. You MUST use this data type, e.g., State.I1

     *** do NOT modify this enum type ***
     */
    public enum State {
        I1, I2, I3, I4, I5, I6, I7, I8
    }

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


}// Scribble class
