package apps.exponent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import external.JSON.JSONException;

import util.http.HttpReader;
import util.http.HttpWriter;
import util.logging.LogSummaryGenerator;

/**
 * The Exponent Summarizer Server is a multi-threaded web server that generates
 * log summaries and sends them back to remote clients. These log summaries should
 * not contain any sensitive data; the summarizer can be queried by anyone and its
 * summaries are made publicly available on the GGP.org viewer alongside the other
 * information about each match.
 * 
 * SAMPLE INVOCATION (when running locally):
 * 
 * ResourceLoader.load_raw('http://127.0.0.1:9199/matchABC');
 * 
 * Exponent Summarizer Server replies with a JSON summary of the logs for "matchABC".
 * 
 * @author Sam Schreiber
 */
public class ExponentSummarizer
{
    public static LogSummaryGenerator theGenerator;
    public static final int SERVER_PORT = 9199;
   
    static class SummarizeLogThread extends Thread {
        private Socket connection;
                
        public SummarizeLogThread(Socket connection) throws IOException, JSONException {
            this.connection = connection;
        }
        
        @Override
        public void run() {
            try {
                String matchId = HttpReader.readAsServer(connection);
                System.out.println("On " + new Date() + ", client has requested: " + matchId);
                String theResponse = theGenerator.getLogSummary(matchId);
                HttpWriter.writeAsServer(connection, theResponse);
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    
    public static void main(String[] args) {
        ServerSocket listener = null;
        try {
             listener = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            System.err.println("Could not open server on port " + SERVER_PORT + ": " + e);
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                Socket connection = listener.accept();
                Thread handlerThread = new SummarizeLogThread(connection);
                handlerThread.start();
            } catch (Exception e) {
                System.err.println(e);
            }
        }        
    }
}