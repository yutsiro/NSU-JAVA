package client;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class ChatUI {
    private JFrame frame;
    private JTextPane chatArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private Map<String, Color> userColors;
    private Runnable onSendMessage;
    private Runnable onRequestUserList;
    private Runnable onLogout;

    public ChatUI() {
        userColors = new HashMap<>();
        initUI();
    }

    private void initUI() {
        frame = new JFrame("Чат-клиент");//окно заголовок
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextPane();//область чата
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        messageField = new JTextField();//ввод соо
        messageField.addActionListener(e -> {
            if (onSendMessage != null) onSendMessage.run();
        });
        frame.add(messageField, BorderLayout.SOUTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        frame.add(new JScrollPane(userList), BorderLayout.EAST);//список юзеров

        JButton listButton = new JButton("Список пользователей");
        listButton.addActionListener(e -> {
            if (onRequestUserList != null) onRequestUserList.run();
        });
        frame.add(listButton, BorderLayout.NORTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onLogout != null) onLogout.run();
            }
        });
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    public void appendMessage(String userName, String message, Color color) {
        StyledDocument doc = chatArea.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();
        //если цвет не указан, используем цвет из userColors
        StyleConstants.setForeground(style, color != null ? color : getUserColor(userName));
        try {
            doc.insertString(doc.getLength(), userName + ": " + message, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void updateUserList(String[] users) {
        userListModel.clear();
        for (String user : users) {
            if (user != null) userListModel.addElement(user);
        }
    }

    public String getMessageText() {
        return messageField.getText().trim();
    }

    public void clearMessageField() {
        messageField.setText("");
    }

    public void setOnSendMessage(Runnable onSendMessage) {
        this.onSendMessage = onSendMessage;
    }

    public void setOnRequestUserList(Runnable onRequestUserList) {
        this.onRequestUserList = onRequestUserList;
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    private Color getUserColor(String userName) {
        return userColors.computeIfAbsent(userName, k -> new Color((int)(Math.random() * 0xFFFFFF)));
    }
}