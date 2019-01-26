package amiin.bazouk.application.com.p2pproject;

import java.io.*;
import java.net.*;
import java.util.*;

public class ProxyServer
{

    public ProxyServer( int port )
    {
        System.out.println( "Proxy Server started at " + new Date() );

        try
        {
            // Create a server socket that will "listen" on specified port
            ServerSocket serverSocket = new ServerSocket( port, 5 );
            //
            //   note that the second argument above (5) is the
            //      number of backlogged clients allowed

            while ( true )
            {
                // Listen for a connection request
                Socket socket = serverSocket.accept();  // BLOCKING !!!!!!!

                System.out.println( "Accepted incoming connection" );

                // Create data input and output streams
                InputStream stream = socket.getInputStream();

                // Add a buffered reader object:
                BufferedReader in =
                        new BufferedReader( new InputStreamReader( stream ) );

                // Read the first line (Request-Line):
                String request = in.readLine();
                request = "GET http://www.cs.rpi.edu/~goldsd HTTP/1.1";
                System.out.println( "Request-Line is: " + request );

                // Isolate the hostname from the URI on the Request-Line:
                //
                //   GET http://www.cs.rpi.edu/~goldsd HTTP/1.1
                //              ^             ^       ^
                //              a             b       c
                //
                int a = request.indexOf( "://" ) + 3;
                int b = request.indexOf( "/", a );
                int c = request.indexOf( " ", b );
                System.out.println("a: " + a );
                System.out.println("b: " + b);
                System.out.println("c: " + c);
                String requestedHost = request.substring( a, b );
                // e.g. "www.cs.rpi.edu"

                String newRequestLine = "GET " + request.substring( b, c ) +
                        " HTTP/1.1";

//        System.out.println( newRequestLine );


                StringBuilder newRequest = new StringBuilder();
                newRequest.append( newRequestLine );
                newRequest.append( "\r\n" );

                newRequest.append( "Host: " );
                newRequest.append( requestedHost );
                newRequest.append( "\r\n" );


                // Read the header line(s):
                String header;
                while ( ( header = in.readLine() ) != null )
                {
                    System.out.println( "RCVD: " + header );

                    // Detect end of header lines (via blank line)
                    if ( header.length() == 0 ) break;   // "\r\n" encountered

                    if ( header.startsWith( "Host:" ) ) continue;
//          if ( header.startsWith( "Connection:" ) ) continue;
                    if ( header.startsWith( "Proxy-Connection:" ) ) continue;

                    newRequest.append( header );
                    newRequest.append( "\r\n" );
                }

//        newRequest.append( "Connection: close\r\n" );
                newRequest.append( "\r\n" );

                System.out.println();
                System.out.println();

                System.out.println( newRequest );



                // Send the new request to the destination host:
                Socket toWebServerSocket = new Socket( requestedHost, 80 );
                OutputStream toWebServerStream =
                        toWebServerSocket.getOutputStream();
                toWebServerStream.write( newRequest.toString().getBytes() );
                toWebServerStream.flush();


                // RECEIVE RESPONSE FROM WEB SERVER
                InputStream fromWebServerStream =
                        toWebServerSocket.getInputStream();

                OutputStream outstream = socket.getOutputStream();
                byte[] rawByte = new byte[1];
                while ( ( fromWebServerStream.read( rawByte, 0, 1 ) ) != -1 )
                {
                    outstream.write( rawByte );
                    outstream.flush();
                }
                System.out.println( "SENT RESPONSE BACK TO CLIENT" );

                if ( false ) {
                    BufferedReader input =
                            new BufferedReader( new InputStreamReader( fromWebServerStream ) );

                    String responseLine = input.readLine();
                    System.out.println( "Response-Line is: " + responseLine );


                    StringBuilder response = new StringBuilder();
                    response.append( responseLine );
                    response.append( "\r\n" );

                    int contentLength = 0;

                    while ( ( header = input.readLine() ) != null )
                    {
                        System.out.println( "RCVD: " + header );

                        if ( header.length() == 0 ) break;

//          if ( header.startsWith( "Connection:" ) ) continue;
                        if ( header.startsWith( "Proxy-Connection:" ) ) continue;

                        response.append( header );
                        response.append( "\r\n" );

                        // "Content-Length: 12345"
                        if ( header.startsWith( "Content-Length:" ) )
                        {
                            String h = header.substring( header.indexOf( ": " ) + 2 );
                            contentLength = Integer.parseInt( h );
                        }
                    }

//        response.append( "Connection: close\r\n" );
                    response.append( "\r\n" );

                    System.out.println( response.toString() );

                    System.out.println( "READING RESPONSE FROM WEB SERVER..." );

                    if ( false ) {
                        byte[] rawData = new byte[contentLength];

                        for ( int q = 0 ; q < contentLength ; q++ )
                        {
                            char cc = (char)input.read();
                            rawData[q] = (byte)cc;
                            System.out.print( "[" + (char)rawData[q] + "]" );
                        }
                        System.out.println();
                        response.append( new String( rawData ) );
                    }

                    StringBuilder rawData = new StringBuilder();
                    for ( int q = 0 ; q < contentLength ; q++ )
                    {
                        char cc = (char)input.read();
                        System.out.print( "[" + cc + "]" );
                        rawData.append( cc );
                    }
                    System.out.println();

                    response.append( rawData );
                    System.out.println( "READ FULL RESPONSE FROM WEB SERVER..." );

//        System.out.println( response.toString() );

                    // send the response back to the client:
                    OutputStream ostream = socket.getOutputStream();
                    ostream.write( response.toString().getBytes() );
//        ostream.write( rawData );
                    ostream.flush();

                    System.out.println( "SENT RESPONSE BACK TO CLIENT" );
                }





            }
        }
        catch( IOException ex )
        {
            System.err.println( ex );
        }
    }
}