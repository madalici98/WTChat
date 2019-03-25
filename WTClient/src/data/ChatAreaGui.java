package data;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ChatAreaGui extends JFrame {
	
	private JTextArea chat;

    public ChatAreaGui() {

        JFrame frame = new JFrame();
        frame.setTitle("Chat Area");
        frame.setBounds(700,100,500,500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);

        chat = new JTextArea(100,30);
        chat.setEditable(false);

        JScrollPane rootPanel = new JScrollPane(chat);
        frame.setContentPane(rootPanel);

        frame.revalidate();
        frame.repaint();
    }

    public JTextArea getChat() {
        return chat;
    }

}
