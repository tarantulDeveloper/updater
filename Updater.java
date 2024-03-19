import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Updater extends JFrame {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private final String oldJarName;
    private final String newJarName;
    private JTextArea logTextArea;
    private final ProcessBuilder processBuilder;
    private final Thread updatedJarThread;

    public Updater(String oldJarName, String newJarName) {
        this.oldJarName = oldJarName;
        this.newJarName = newJarName;
        processBuilder = new ProcessBuilder("java", "-jar", newJarName);
        updatedJarThread = new Thread(() -> {
            try{
                processBuilder.start();
            } catch (IOException ioe) {
                System.out.println(ioe);
            }

        });

        initializeUI();
        downloadNewJar();
    }

    private void initializeUI() {
        setTitle("Updater");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setBounds(0,0,500,40);

        statusLabel = new JLabel("Downloading...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBounds(0, 40, 500, 40);

        logTextArea = new JTextArea();
        logTextArea.setEditable(false); // Make it non-editable
        JScrollPane scrollPane = new JScrollPane(logTextArea,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBounds(0, 80, 500, 375);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);
        JPanel panel = new JPanel();
        panel.setLayout(null);
        setResizable(false);

        panel.add(progressBar);
        panel.add(statusLabel);
        panel.add(scrollPane);

        getContentPane().add(panel);
    }

    private void downloadNewJar() {
        String downloadUrl = "http://localhost:8820/api/updater/download/jar";

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int fileSize = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(newJarName);

            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                int percentage = (int) (totalBytesRead * 100 / fileSize);
                progressBar.setValue(percentage);
            }

            inputStream.close();
            outputStream.close();

            statusLabel.setText("Download Complete");

            shutdownServer();

            // Delete old jar
            boolean deleteResult = new File(oldJarName).delete();
            if (!deleteResult) {
                statusLabel.setText("Error: Unable to delete old JAR");
            } else {
                statusLabel.setText("Old JAR Deleted Successfully");
            }

            updatedJarThread.start();


        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void shutdownServer() {
        try {
            Process process = Runtime.getRuntime().exec("cmd /c netstat -ano | findstr :7720");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            List<String> pidList = new ArrayList<String>();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 0) {
                    String pid = parts[parts.length - 1];
                    pidList.add(pid);
                }
            }

            for (String pid : pidList) {
                logTextArea.append("Found server process with PID: " + pid + "\n");

                // Terminate the process
                Process killProcess = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                BufferedReader killReader = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));
                String killOutput;
                while ((killOutput = killReader.readLine()) != null) {
                    logTextArea.append(killOutput + "\n");
                }
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            logTextArea.append("Error: " + ex.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java UpdaterApp <old_jar_name>");
            System.exit(1);
        }
        
        SwingUtilities.invokeLater(() -> {
            String oldJarName = args[0];
            String newJarName = args[1];
            new Updater(oldJarName, newJarName).setVisible(true);
        });
        new Updater("old.jar", "new.jar").setVisible(true);
    }

}
