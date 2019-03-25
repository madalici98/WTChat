package data;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.*;

import WTUtils.ServerRequest;
import WTUtils.ServerCommand;
import WTUtils.RequestTypes;

import static WTUtils.RequestTypes.HAND_DOWN;
import static WTUtils.RequestTypes.SEND_MESSAGE;
import static java.lang.System.exit;


public class LaunchClient {
	
	private static JTextField nameInput = new JTextField();
    private static JButton setName;

    private static JButton raiseHand;
    private static JButton lowerHand;

    private static JTextField userMessage;
    private static JButton sendMessage;
    
    private static ChatAreaGui sg;

    private static Socket sock;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;

    private static int handUpTime;

    private static Boolean hasHandDown = true;

	public static void main(String args[]) throws ClassNotFoundException, IOException {
		
		System.out.println("I just launched\n");

		/*the GUI part*/
        sg = new ChatAreaGui();

		JFrame frame = new JFrame();
        frame.setTitle("Walkie Talkie");
        frame.setBounds(100,100,500,500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(0);

        JPanel rootPanel = new JPanel(new GridBagLayout());
        frame.setContentPane(rootPanel);

        JPanel contentContainer = new JPanel(new GridLayout(3,2));

        nameInput.setEditable(true);
        contentContainer.add(nameInput);
        nameInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                //no need of this
            }

            @Override
            public void keyPressed(KeyEvent e) {

                if(e.getKeyCode() == KeyEvent.VK_ENTER) {

                    trySetName();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                //no need of this
            }
        });
        
        setName = new JButton("Set username");
        contentContainer.add(setName);
        setName.addActionListener(e -> trySetName());
        
        raiseHand = new JButton("Raise the hand!");
        raiseHand.setEnabled(false);
        contentContainer.add(raiseHand);
        raiseHand.addActionListener(e -> handUp());
        
        lowerHand = new JButton("Lower the hand!");
        lowerHand.setEnabled(false);
        contentContainer.add(lowerHand);
        lowerHand.addActionListener(e -> handDown(HAND_DOWN));
        
        userMessage = new JTextField();
        userMessage.setEditable(false);
        userMessage.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                //no need of this
            }

            @Override
            public void keyPressed(KeyEvent e) {

                if(e.getKeyCode() == KeyEvent.VK_ENTER) {

                    send();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                //no need of this
            }
        });
        contentContainer.add(userMessage);

        sendMessage = new JButton("Send");
        sendMessage.setEnabled(false);
        contentContainer.add(sendMessage);
        sendMessage.addActionListener(e -> send());
        
        rootPanel.add(contentContainer);

        frame.revalidate();
        frame.repaint();

        sg.getChat().append("Set an username to start the communication session\n");

		try {

            sock = new Socket("localhost", 12345);

            oos = new ObjectOutputStream (sock.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream (sock.getInputStream());

        } catch (ConnectException e2) {

            e2.printStackTrace();
            sg.getChat().append("Can not connect to the server\n");
            //do a special pop up window to notify the client with CLOSE_ON_EXIT
            //hide the chat windows

        } catch (IOException e1) {
		
			e1.printStackTrace();
			sg.getChat().append("Something went wrong\n");
		}
        /*end of GUI part*/

        while (true) {

            try {

                ServerCommand command = (ServerCommand) ois.readObject();

                switch (command.getCommType()) {

                    case DISPLAY_MESSAGE:
                        //display a message in the chat area
                        sg.getChat().append((String)command.getData());
                        break;

                    case RAISE_HAND_ACCEPTED:
                        raiseHand.setEnabled(false);
                        lowerHand.setEnabled(true);
                        userMessage.setEditable(true);
                        sendMessage.setEnabled(true);
                        hasHandDown = false;

                        sg.getChat().append("You've raised your hand. You have " + handUpTime / 1000 + " seconds to speak your mind.\n");

                        Timer timer = new Timer(handUpTime, new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                //nu imi place deloc asta; cauta solutie
                                if(!hasHandDown) {

                                    sg.getChat().append("Your time is up.\n");
                                    handDown(HAND_DOWN);
                                }
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                        //more things to happen
                        break;

                    case RAISE_HAND_REJECTED:
                        sg.getChat().append("Your request to raise your hand was rejected.\n");
                        //more things to happen
                        break;

                    case LOWER_HAND_ACCEPTED:
                        lowerHandGUI();
                        sg.getChat().append("You lowered your hand.\n");
                        break;

                    case NAME_ACCEPTED:
                        sg.getChat().append("Name accepted. Connection established\n");
                        handUpTime = (Integer) command.getData();
                        nameInput.setEditable(false);
                        setName.setEnabled(false);
                        raiseHand.setEnabled(true);
                        break;

                    case NAME_REJECTED:
                        sg.getChat().append("Connection rejected. The username is invalid/already in use. Please retry.\n");
                        break;

                    default:
                        System.out.println("Weird things are happening\n");
                }

            } catch (SocketException e1) {

                exit(0);
            }
        } 
	}

	//enables and disables the buttons and textfields used for sending messages
	private static void lowerHandGUI() {

        raiseHand.setEnabled(true);
        lowerHand.setEnabled(false);
        userMessage.setEditable(false);
        sendMessage.setEnabled(false);
        hasHandDown = true;
    }

    //sends the user's messages to the server
	private static void send() {

	    String message = userMessage.getText();
	    if (message.isEmpty()) return; //i don't want to send an empty message; it has no sense
	    userMessage.setText("");

	    ServerRequest sr = new ServerRequest();
	    sr.setData(message);
	    sr.setReqType(SEND_MESSAGE);
	    sendRequest(sr);
    }

    //sends to the server the request to set the name, as well as the name desired by the user
	private static void trySetName() {
		
		String clientMessage;

        try {
            clientMessage = nameInput.getText();
            if(clientMessage.length() < 1) {

                sg.getChat().append("Write your name in  the box\n");
                return;
            }
            ServerRequest sr = new ServerRequest();
            sr.setReqType(RequestTypes.SET_NAME);
            sr.setData(clientMessage);
            sendRequest(sr);

        } catch (NullPointerException e) {

            sg.getChat().append("Write your name in the box\n");
        }
		
	}

	//sends a request to the server
    private static void sendRequest(ServerRequest req) {

        try {
            oos.writeObject(req);
            oos.flush();
            oos.reset();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //sends to the server the hand down request
	private static void handUp() {
		
		ServerRequest sr = new ServerRequest();
        sr.setReqType(RequestTypes.HAND_UP);
        sendRequest(sr);
	}

	//sends to the server the two possible requests in this situation:
    //HAND_DOWN: if the clients lowered his hand pressing the button
    //TIME_UP: the client's allocated time has expired
	private static void handDown(RequestTypes rt) {
		
		ServerRequest sr = new ServerRequest();
        sr.setReqType(rt);
        sendRequest(sr);
	}

}
