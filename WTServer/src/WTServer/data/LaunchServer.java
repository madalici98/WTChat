package WTServer.data;

import WTUtils.ServerCommand;
import WTUtils.ServerRequest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static WTUtils.CommandTypes.*;
import static java.lang.System.exit;

public class LaunchServer {
	
	private static Map<String, ClientThread> connections = new HashMap<String, ClientThread>();
    //private static Boolean anyHandRaised = false; //no one has the control over the chat yet; only one user at the time can have it (project requirement)
    private static int handUpTime;
    private static ClientThread hasHandRaised = null; //it is good to know who has the raised hand

    public static void registerNewClient(ClientThread ct) {

        connections.put(ct.getUserName(),ct);
        System.out.println("User registered: " + ct.getUserName());

    }

    public static void lowerHand(ClientThread ct) {

        hasHandRaised = null;
        ServerCommand sc = new ServerCommand();
        sc.setCommType(LOWER_HAND_ACCEPTED);
        ct.sendCommand(sc);
    }

	public static void main(String[] args) {

        try (ServerSocket ss = new ServerSocket(12345)) {

            // I'm getting the hand up time from the .ini file
            Config cfg = new Config("E:/Facultate/PAO/WTServer/src/WTServer/data/serverInfo.ini");
            handUpTime = Integer.parseInt(cfg.getProperty("handUpTime")) * 1000; //I turned it into nanoseconds
            System.out.println("Server up");
            
            while(true) {

                new ClientThread(ss.accept()).start();
                System.out.println("A new client has connected");

            }

        } catch (FileNotFoundException e1) {
            System.out.println("Unable to find the configuration file");
            exit(0);

        } catch (IOException e2) {
            e2.printStackTrace();
        }
	}

    private static class ClientThread extends Thread {

        private String userName;
        private Socket sock;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public ClientThread(Socket s) throws IOException {

            this.sock = s;
            ois = new ObjectInputStream(sock.getInputStream());
            oos = new ObjectOutputStream(sock.getOutputStream());
            oos.flush();
        }

        public String getUserName() {

            return userName;
        }

        public ObjectOutputStream getOos() {

            return this.oos;
        }

        public void setUserName(String s) {

            this.userName = s;
        }

        private void sendCommand(ServerCommand comm) {

            try {

                oos.writeObject(comm);
                oos.flush();
                oos.reset();

            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        private void broadcastMessage(String message, Boolean ignoreThis) {

            Set<Map.Entry<String, ClientThread>> clients = connections.entrySet();

            ServerCommand sc = new ServerCommand();
            sc.setCommType(DISPLAY_MESSAGE);
            sc.setData(message + "\n");

            try {

                for (Map.Entry<String, ClientThread> ct: clients) {

                        if (ignoreThis == true && ct.getValue() == this) continue;
                        ObjectOutputStream oos = ct.getValue().getOos();
                        oos.writeObject(sc);
                        oos.flush();
                        oos.reset();

                }

            } catch (IOException e) {

                e.printStackTrace();
            }

        }//end of BroadcastMessage

        public void run() {

            while (true) {

                try {

                    ServerRequest sr = (ServerRequest) ois.readObject();
                    ServerCommand sc = new ServerCommand();

                    switch (sr.getReqType()) {

                        case HAND_UP:
                            //things to happen : check if the user can raise his hand; if he can
                            //leave him communicate with the server for a certain amount of time
                            //to do: avoid race conditions; use a synchronised block/method
                            synchronized (ClientThread.class) {

                                if (hasHandRaised == null) {
                                    //the chat is free; the user can start sending his messages
                                    //notify the user he can start sending his messages
                                    Boolean x = true;
                                    hasHandRaised = this;
                                    sc.setCommType(RAISE_HAND_ACCEPTED);
                                    sendCommand(sc);

                                } else {
                                    //the chat is not free
                                    //notify the user
                                    sc.setCommType(RAISE_HAND_REJECTED);
                                    sendCommand(sc);
                                }

                            }//end of synchronised block
                            break;

                        case HAND_DOWN:
                            //things to happen : stop the user message sending session
                            //notify everyone that the chanel is free again
                            lowerHand(this);
                            break;

                        case SET_NAME:
                            //things to happen : check if the name desired by the user is available
                            //set it if possible; add the user to the connection hashmap
                            synchronized (this) {
                            String n = (String) sr.getData();
                            Set<String> userNames = connections.keySet();

                                if (userNames.contains(n)) {
                                    //inform the user that his name is not available
                                    sc.setCommType(NAME_REJECTED);
                                    sendCommand(sc);
                                    System.out.println(n + " already exists");

                                } else {
                                    //the name is available
                                    //add the user to the connections hash map
                                    //notify the user that his name is good
                                    sc.setCommType(NAME_ACCEPTED);
                                    sc.setData(handUpTime); //inform the client about how much time he has to send his messages
                                    sendCommand(sc);
                                    this.setUserName(n);
                                    registerNewClient(this);
                                    broadcastMessage("User registered: " + this.getUserName(), true);
                                }
                            }
                            break;

                        case SEND_MESSAGE:
                            //things to happen : send to everyone the message sent by the user
                            broadcastMessage(new SimpleDateFormat("HH.mm").format(new Date())
                                    + ", " + this.userName + ": " + (String)sr.getData(), false);
                            break;

                        default:
                            System.out.println("Something went wrong");
                    }//end of switch

                } catch (SocketException e) {

                    //delete the disconected client from the connections hash map
                    String aux = "User disconected: " + this.userName;
                    System.out.println(aux);
                    broadcastMessage(aux, true);
                    if (hasHandRaised == this) {

                        hasHandRaised = null;
                    }
                    connections.remove(this.userName);
                    break;

                } catch (IOException | ClassNotFoundException e) {

                    e.printStackTrace();

                } //end of try-catch block

            } //end of while

        }//end of run

    }//end of ClientThread class declaration

}//end of Launchserver class declaration