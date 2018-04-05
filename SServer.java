/* Server program for the Scribble app

   @author YOUR FULL NAME GOES HERE

   @version CS 391 - Spring 2018 - A3
*/

import java.net.*;
import java.io.*;

public class SServer {

    static ServerSocket serverSocket = null;   // listening socket
    static int portNumber = 55555;             // port on which server listens
    static int seed;                           // seed for controlling the
                                               // randomness of the game

    /* Start the server, then repeatedly wait for and accept two connection
       requests and start a new Scribble thread to handle one game between
       the two corresponding players. Before starting the thread, this method
       sends a <welcome + wait prompt> message to both players. Each successive
       thread is passed a seed value, starting with the seed 0 for the first
       thread, the seed 1 for the second thread, etc.
       The output sent to the console by this method is described in the handout.
    */
    public static void main(String[] args) {
    	try {
            serverSocket = new ServerSocket(portNumber);
	        System.out.println("Server started: " + serverSocket);

	        while (true) {
		        Socket clientSocketOne = serverSocket.accept();
		        System.out.println("First player connected: " + 
				   clientSocketOne);
		        DataOutputStream out1 = new DataOutputStream(clientSocketOne.getOutputStream());
		        out1.writeUTF("Welcome to Scribble!\n\nPlease wait for your opponent...");
		        
		        Socket clientSocketTwo = serverSocket.accept();
		        System.out.println("Second player connected: " + 
				   clientSocketTwo);
		        DataOutputStream out2 = new DataOutputStream(clientSocketTwo.getOutputStream());
		        out2.writeUTF("Welcome to Scribble!\n\nPlease wait for your opponent...");
		        
		        (new Thread( new Scribble(clientSocketOne, clientSocketTwo, seed))).start();
	         }
        } catch (IOException e) {
            System.out.println("Server encountered an error. "
            		            + "Shutting down...");
        }


    }// main method

}// SServer class
