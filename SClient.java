/* Client program for the Scribble app

   @author <YOUR FULL NAME GOES HERE>

   @version CS 391 - Spring 2018 - A3
*/

import java.io.*;
import java.net.*;

public class SClient {

    static String hostName = "localhost"; // name of server machine
    static int portNumber = 55555;        // port on which server listens
    static Socket socket = null;          // socket to server
    static DataInputStream in = null;     // input stream from server
    static DataOutputStream out = null;   // output stream to server
    static BufferedReader console = null; // keyboard input stream

    /* connect to the server, open needed I/O streams, read in and display the
       welcome message from the server, play a game, and clean up
     */
    public static void main(String[] args) {
        String query, reply;

      try {
          socket = new Socket(hostName, portNumber);
          System.out.println("Connected to server: " + socket);
          openStreams();
          reply = in.readUTF();
          System.out.println(reply);
          reply = in.readUTF();
          System.out.println(reply);
          query = console.readLine();
          out.writeUTF(query);
          reply = in.readUTF();
          System.out.println(reply);
          playGame();
          close();
      } catch(UnknownHostException e) {
          System.err.println("Unknown host: " + hostName);
          System.exit(1);
      } catch(IOException e) {
          System.err.println("I/O error when connecting to" +
          hostName);
          System.exit(1);
      }

    }// main method

    /* open the necessary I/O streams and initialize the in, out, and console
       static variables; this method does not catch any exceptions.
     */
    static void openStreams() throws IOException {

      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
      console = new BufferedReader(
              new InputStreamReader(System.in)
      );

    }// openStreams method

    /* close all open I/O streams and sockets
     */
    static void close() {
        try {
            if (console != null) { console.close(); }
            if (in != null)      { in.close();      }
            if (out != null)     { out.close();     }
            if (socket != null)  { socket.close();  }
    } catch (IOException e) {
        System.err.println("Error in close(): " +
                e.getMessage());
    }

    }// close method

    /* implement the Scribble client FSM given in the handout. The output
       sent to the console by this method is also specified in the handout,
       namely in the provided traces.
     */
    static void playGame() {
        try {
            String reply, query;
            while (true) {
                reply = in.readUTF();
                System.out.println(reply);
                if (reply.contains("GAME OVER")) {
                    break;
                }
                query = console.readLine();
                while (!query.equalsIgnoreCase("A") || !query.equalsIgnoreCase("D")) {
                    query = console.readLine();
                }
                out.writeUTF(query);
                reply = in.readUTF();
                System.out.println(reply);
                query = console.readLine();
                boolean formatCorrect = false;
                while (!formatCorrect) {
                    if (((int) query.charAt(0) >= 65) && (int) query.charAt(0) <= 74) {
                        if (((int) query.charAt(1) >= 48) && (int) query.charAt(1) <= 57) {
                            formatCorrect = true;
                        }
                    }
                    if (!formatCorrect) {
                        query = console.readLine();
                    }
                }
                out.writeUTF(query);
                reply = in.readUTF();
                System.out.println(reply);
                query = console.readLine();
                out.writeUTF(query.toUpperCase());
            }
        } catch(UnknownHostException e) {
            System.err.println("Unknown host: " + hostName);
            System.exit(1);
        } catch(IOException e) {
            System.err.println("I/O error when connecting to" +
                    hostName);
            System.exit(1);
        }

    }// playGame method

    /* states of the client FSM, which you must usee; refer to them
       as State.C1, say, in your code

       *** do NOT modify this data type ***
     */
    public enum State { C1, C2, C3, C4, C5, C6 }

}// SClient class
